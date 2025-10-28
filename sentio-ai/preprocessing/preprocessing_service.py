from fastapi import FastAPI, File, UploadFile, Form, HTTPException
from fastapi.responses import JSONResponse
import cv2
import numpy as np
import httpx
import logging
import os
from pathlib import Path
from datetime import datetime
from typing import Dict, Any
from dotenv import load_dotenv

# Load environment variables
load_dotenv()

# Configure logging
log_level = os.getenv('LOG_LEVEL', 'INFO')
logging.basicConfig(level=getattr(logging, log_level))
logger = logging.getLogger(__name__)

app = FastAPI(title="Animal Detection Preprocessing Service")

# Configuration from environment variables
SERVICE_HOST = os.getenv('SERVICE_HOST', '0.0.0.0')
SERVICE_PORT = int(os.getenv('SERVICE_PORT', '8082'))
BIRD_CLASSIFIER_URL = os.getenv('BIRD_CLASSIFIER_URL', 'http://localhost:8000/detect')
SPECIES_CLASSIFIER_URL = os.getenv('SPECIES_CLASSIFIER_URL', 'http://localhost:8081/detect')

# Image saving configuration
SAVE_IMAGES = os.getenv('SAVE_IMAGES', 'true').lower() == 'true'
SAVE_DIRECTORY = os.getenv('SAVE_DIRECTORY', './enhanced_images')

# Enhancement thresholds
NOISE_THRESHOLD = float(os.getenv('NOISE_THRESHOLD', '50'))
BLUR_THRESHOLD = float(os.getenv('BLUR_THRESHOLD', '100'))
LOW_CONTRAST_THRESHOLD = float(os.getenv('LOW_CONTRAST_THRESHOLD', '30'))
OVEREXPOSURE_THRESHOLD = float(os.getenv('OVEREXPOSURE_THRESHOLD', '180'))
UNDEREXPOSURE_THRESHOLD = float(os.getenv('UNDEREXPOSURE_THRESHOLD', '75'))

# Enhancement parameters
DENOISE_STRENGTH = int(os.getenv('DENOISE_STRENGTH', '10'))
CLAHE_CLIP_LIMIT = float(os.getenv('CLAHE_CLIP_LIMIT', '2.0'))
CONTRAST_BOOST = float(os.getenv('CONTRAST_BOOST', '1.1'))
SATURATION_BOOST = float(os.getenv('SATURATION_BOOST', '1.05'))

# Create save directory if saving is enabled
if SAVE_IMAGES:
    Path(SAVE_DIRECTORY).mkdir(parents=True, exist_ok=True)
    logger.info(f"Image saving enabled. Directory: {SAVE_DIRECTORY}")
else:
    logger.info("Image saving disabled")


class ImagePreprocessor:
    """
    Handles image preprocessing including denoising, deblurring, and exposure correction.
    """

    @staticmethod
    def enhance_image(image_bytes: bytes) -> bytes:
        """
        Apply comprehensive image enhancement pipeline with intelligent quality detection.

        Args:
            image_bytes: Raw image bytes

        Returns:
            Enhanced image bytes
        """
        try:
            # Convert bytes to numpy array
            nparr = np.frombuffer(image_bytes, np.uint8)
            img = cv2.imdecode(nparr, cv2.IMREAD_COLOR)

            if img is None:
                logger.error("Failed to decode image")
                return image_bytes

            logger.info(f"Original image shape: {img.shape}")

            # Assess image quality to determine if enhancement is needed
            needs_enhancement = ImagePreprocessor._assess_image_quality(img)

            if not needs_enhancement['any']:
                logger.info("Image quality is good, skipping enhancement")

                # Save original only if enabled
                if SAVE_IMAGES:
                    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S_%f")
                    original_path = os.path.join(SAVE_DIRECTORY, f"original_{timestamp}.jpg")
                    cv2.imwrite(original_path, img)
                    logger.info(f"Saved original (no enhancement needed): {original_path}")

                return image_bytes

            logger.info(f"Enhancement needed: {needs_enhancement}")
            result = img.copy()

            # 1. Denoise only if noisy
            if needs_enhancement['noisy']:
                result = cv2.fastNlMeansDenoisingColored(result, None, DENOISE_STRENGTH, DENOISE_STRENGTH, 7, 21)
                logger.debug("Applied denoising")

            # 2. Correct exposure only if needed
            if needs_enhancement['bad_exposure']:
                result = ImagePreprocessor._correct_exposure(result, needs_enhancement['mean_brightness'])
                logger.debug("Applied exposure correction")

            # 3. Sharpen only if blurry
            if needs_enhancement['blurry']:
                result = ImagePreprocessor._sharpen_image(result, needs_enhancement['blur_metric'])
                logger.debug("Applied sharpening")

            # 4. Enhance contrast only if low contrast
            if needs_enhancement['low_contrast']:
                result = ImagePreprocessor._enhance_contrast(result)
                logger.debug("Applied contrast enhancement")

            # Save original and enhanced images if enabled
            if SAVE_IMAGES:
                timestamp = datetime.now().strftime("%Y%m%d_%H%M%S_%f")
                original_path = os.path.join(SAVE_DIRECTORY, f"original_{timestamp}.jpg")
                enhanced_path = os.path.join(SAVE_DIRECTORY, f"enhanced_{timestamp}.jpg")

                cv2.imwrite(original_path, img)
                cv2.imwrite(enhanced_path, result)
                logger.info(f"Saved images: {original_path}, {enhanced_path}")

            # Convert back to bytes
            success, encoded_image = cv2.imencode('.jpg', result)
            if not success:
                logger.error("Failed to encode enhanced image")
                return image_bytes

            logger.info("Image enhancement completed successfully")
            return encoded_image.tobytes()

        except Exception as e:
            logger.error(f"Error during image enhancement: {e}", exc_info=True)
            return image_bytes  # Return original if enhancement fails

    @staticmethod
    def _assess_image_quality(img: np.ndarray) -> Dict[str, Any]:
        """
        Assess image quality to determine what enhancements are needed.

        Returns:
            Dictionary with quality metrics and flags for needed enhancements
        """
        gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)

        # 1. Check for noise (using edge detection and variance)
        edges = cv2.Canny(gray, 50, 150)
        noise_metric = np.std(gray)
        is_noisy = noise_metric > NOISE_THRESHOLD

        # 2. Check exposure
        lab = cv2.cvtColor(img, cv2.COLOR_BGR2LAB)
        l_channel = lab[:, :, 0]
        mean_brightness = np.mean(l_channel)
        bad_exposure = mean_brightness > OVEREXPOSURE_THRESHOLD or mean_brightness < UNDEREXPOSURE_THRESHOLD

        # 3. Check blur (Laplacian variance)
        blur_metric = cv2.Laplacian(gray, cv2.CV_64F).var()
        is_blurry = blur_metric < BLUR_THRESHOLD

        # 4. Check contrast (standard deviation of luminance)
        contrast_metric = np.std(l_channel)
        low_contrast = contrast_metric < LOW_CONTRAST_THRESHOLD

        needs_any = is_noisy or bad_exposure or is_blurry or low_contrast

        logger.debug(f"Quality assessment - Noise: {noise_metric:.2f}, "
                     f"Brightness: {mean_brightness:.2f}, "
                     f"Blur: {blur_metric:.2f}, "
                     f"Contrast: {contrast_metric:.2f}")

        return {
            'any': needs_any,
            'noisy': is_noisy,
            'bad_exposure': bad_exposure,
            'blurry': is_blurry,
            'low_contrast': low_contrast,
            'noise_metric': noise_metric,
            'mean_brightness': mean_brightness,
            'blur_metric': blur_metric,
            'contrast_metric': contrast_metric
        }

    @staticmethod
    def _correct_exposure(img: np.ndarray, mean_brightness: float) -> np.ndarray:
        """
        Correct overexposed or underexposed images using adaptive histogram equalization.
        """
        # Convert to LAB color space
        lab = cv2.cvtColor(img, cv2.COLOR_BGR2LAB)
        l, a, b = cv2.split(lab)

        logger.debug(f"Mean brightness: {mean_brightness}")

        # Apply CLAHE (Contrast Limited Adaptive Histogram Equalization) to L channel
        clahe = cv2.createCLAHE(clipLimit=CLAHE_CLIP_LIMIT, tileGridSize=(8, 8))

        # Adjust based on exposure level
        if mean_brightness > OVEREXPOSURE_THRESHOLD:  # Overexposed
            logger.debug("Image appears overexposed, applying correction")
            l = clahe.apply(l)
            # Additional darkening for severe overexposure
            l = np.clip(l * 0.85, 0, 255).astype(np.uint8)
        elif mean_brightness < UNDEREXPOSURE_THRESHOLD:  # Underexposed
            logger.debug("Image appears underexposed, applying correction")
            l = clahe.apply(l)
            # Additional brightening for severe underexposure
            l = np.clip(l * 1.15, 0, 255).astype(np.uint8)

        # Merge channels and convert back to BGR
        enhanced_lab = cv2.merge([l, a, b])
        enhanced = cv2.cvtColor(enhanced_lab, cv2.COLOR_LAB2BGR)

        return enhanced

    @staticmethod
    def _sharpen_image(img: np.ndarray, blur_metric: float) -> np.ndarray:
        """
        Sharpen image to reduce blur using unsharp masking.
        """
        logger.debug(f"Blur metric: {blur_metric}")

        # If image is very blurry (low variance), apply stronger sharpening
        if blur_metric < 50:
            logger.debug("Image appears very blurry, applying strong sharpening")
            # Gaussian blur
            gaussian = cv2.GaussianBlur(img, (0, 0), 3.0)
            # Unsharp masking
            sharpened = cv2.addWeighted(img, 1.5, gaussian, -0.5, 0)
        else:
            # Apply mild sharpening
            logger.debug("Image appears slightly blurry, applying mild sharpening")
            kernel = np.array([[0, -1, 0],
                               [-1, 5, -1],
                               [0, -1, 0]])
            sharpened = cv2.filter2D(img, -1, kernel)

        return sharpened

    @staticmethod
    def _enhance_contrast(img: np.ndarray) -> np.ndarray:
        """
        Enhance overall image contrast using OpenCV (mild enhancement).
        """
        # Convert to HSV for better color handling
        hsv = cv2.cvtColor(img, cv2.COLOR_BGR2HSV)
        h, s, v = cv2.split(hsv)

        # Mild contrast enhancement
        v = np.clip(v * CONTRAST_BOOST, 0, 255).astype(np.uint8)

        # Mild color saturation
        s = np.clip(s * SATURATION_BOOST, 0, 255).astype(np.uint8)

        # Merge channels and convert back to BGR
        enhanced_hsv = cv2.merge([h, s, v])
        enhanced_bgr = cv2.cvtColor(enhanced_hsv, cv2.COLOR_HSV2BGR)

        return enhanced_bgr


async def forward_to_classifier(
        enhanced_image_bytes: bytes,
        animal_type: str,
        original_filename: str
) -> Dict[str, Any]:
    """
    Forward the preprocessed image to the appropriate classifier service.

    Args:
        enhanced_image_bytes: Preprocessed image bytes
        animal_type: Type of animal detected (bird, mammal, etc.)
        original_filename: Original filename for the request

    Returns:
        Classification response from the downstream service
    """
    # Determine target URL based on animal type
    if animal_type.lower() == "bird":
        target_url = BIRD_CLASSIFIER_URL
        logger.info(f"Routing to bird classifier: {target_url}")
    elif animal_type.lower() in ["mammal", "human"]:  # human for testing
        target_url = SPECIES_CLASSIFIER_URL
        logger.info(f"Routing to species classifier: {target_url}")
    else:
        logger.warning(f"Unknown animal type '{animal_type}', defaulting to species classifier")
        target_url = SPECIES_CLASSIFIER_URL

    try:
        # Prepare multipart form data
        files = {
            'file': (original_filename, enhanced_image_bytes, 'image/jpeg')
        }

        # Forward to classifier with timeout
        async with httpx.AsyncClient(timeout=30.0) as client:
            logger.info(f"Forwarding preprocessed image to {target_url}")
            response = await client.post(target_url, files=files)
            response.raise_for_status()

            result = response.json()
            logger.info(f"Received classification response from {target_url}")
            return result

    except httpx.HTTPError as e:
        logger.error(f"HTTP error when forwarding to classifier: {e}", exc_info=True)
        raise HTTPException(
            status_code=502,
            detail=f"Error communicating with classifier service: {str(e)}"
        )
    except Exception as e:
        logger.error(f"Unexpected error when forwarding to classifier: {e}", exc_info=True)
        raise HTTPException(
            status_code=500,
            detail=f"Internal error during classification: {str(e)}"
        )


@app.get("/health")
async def health_check():
    """Health check endpoint."""
    return {"status": "healthy", "service": "preprocessing-service"}


@app.post("/preprocess-and-classify")
async def preprocess_and_classify(
        file: UploadFile = File(...),
        animal_type: str = Form(...)
):
    """
    Main endpoint: Preprocess image and forward to appropriate classifier.

    Args:
        file: Image file to process
        animal_type: Type of animal (bird, mammal, etc.)

    Returns:
        Classification results from the downstream service
    """
    logger.info(f"Received preprocessing request for animal_type: {animal_type}")
    logger.info(f"Original filename: {file.filename}")

    try:
        # Read image bytes
        image_bytes = await file.read()
        logger.info(f"Received image of size: {len(image_bytes)} bytes")

        # Preprocess the image
        logger.info("Starting image preprocessing...")
        enhanced_image_bytes = ImagePreprocessor.enhance_image(image_bytes)
        logger.info(f"Preprocessing complete. Enhanced image size: {len(enhanced_image_bytes)} bytes")

        # Forward to appropriate classifier
        classification_result = await forward_to_classifier(
            enhanced_image_bytes,
            animal_type,
            file.filename or "image.jpg"
        )

        # Add preprocessing metadata to response
        classification_result["preprocessing_applied"] = True
        classification_result["original_size_bytes"] = len(image_bytes)
        classification_result["enhanced_size_bytes"] = len(enhanced_image_bytes)

        logger.info("Request processed successfully")
        return JSONResponse(content=classification_result)

    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Error processing request: {e}", exc_info=True)
        raise HTTPException(
            status_code=500,
            detail=f"Error processing image: {str(e)}"
        )


@app.post("/preprocess-only")
async def preprocess_only(file: UploadFile = File(...)):
    """
    Endpoint for testing: Only preprocess the image without classification.
    Returns the enhanced image.
    """
    logger.info("Received preprocessing-only request")

    try:
        # Read image bytes
        image_bytes = await file.read()

        # Preprocess the image
        enhanced_image_bytes = ImagePreprocessor.enhance_image(image_bytes)

        from fastapi.responses import Response
        return Response(
            content=enhanced_image_bytes,
            media_type="image/jpeg",
            headers={
                "X-Original-Size": str(len(image_bytes)),
                "X-Enhanced-Size": str(len(enhanced_image_bytes))
            }
        )

    except Exception as e:
        logger.error(f"Error preprocessing image: {e}", exc_info=True)
        raise HTTPException(
            status_code=500,
            detail=f"Error preprocessing image: {str(e)}"
        )


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(app, host=SERVICE_HOST, port=SERVICE_PORT, log_level=log_level.lower())
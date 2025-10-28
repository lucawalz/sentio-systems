import { useEffect, useRef, useState } from "react"
import { motion, useScroll, useTransform, useInView } from "framer-motion"
import { Button } from "../ui/button"
import { ChevronDown, Play } from "lucide-react"

export function Hero() {
    const heroRef = useRef<HTMLDivElement>(null)
    const videoRef = useRef<HTMLVideoElement>(null)
    const canvasRef = useRef<HTMLCanvasElement>(null)

    const titleRef = useRef<HTMLHeadingElement>(null)
    const subtitleRef = useRef<HTMLParagraphElement>(null)
    const ctaRef = useRef<HTMLDivElement>(null)

    const titleInView = useInView(titleRef, { once: true })
    const subtitleInView = useInView(subtitleRef, { once: true })
    const ctaInView = useInView(ctaRef, { once: true })

    const [videoLoaded, setVideoLoaded] = useState(false)
    const [videoError, setVideoError] = useState(false)

    const { scrollYProgress } = useScroll({
        target: heroRef,
        offset: ["start start", "end start"]
    })

    const y = useTransform(scrollYProgress, [0, 1], ["0%", "-50%"])
    const opacity = useTransform(scrollYProgress, [0, 1], [1, 0.3])

    useEffect(() => {
        // Only run canvas animation if video failed to load
        if (!videoLoaded && videoError && canvasRef.current) {
            const canvas = canvasRef.current
            const ctx = canvas.getContext("2d")
            if (ctx) {
                animateCanvas(ctx, canvas)
            }
        }
    }, [videoLoaded, videoError])

    const animateCanvas = (ctx: CanvasRenderingContext2D, canvas: HTMLCanvasElement) => {
        canvas.width = window.innerWidth
        canvas.height = window.innerHeight

        const particles: Array<{
            x: number
            y: number
            vx: number
            vy: number
            size: number
            opacity: number
            color: string
        }> = []

        // Create particles
        for (let i = 0; i < 50; i++) {
            particles.push({
                x: Math.random() * canvas.width,
                y: Math.random() * canvas.height,
                vx: (Math.random() - 0.5) * 0.5,
                vy: (Math.random() - 0.5) * 0.5,
                size: Math.random() * 3 + 1,
                opacity: Math.random() * 0.5 + 0.1,
                color: ["#B0D6FF", "#A8D5BA", "#FFD8A8"][Math.floor(Math.random() * 3)],
            })
        }

        const animate = () => {
            ctx.clearRect(0, 0, canvas.width, canvas.height)

            particles.forEach((particle) => {
                particle.x += particle.vx
                particle.y += particle.vy

                if (particle.x < 0 || particle.x > canvas.width) particle.vx *= -1
                if (particle.y < 0 || particle.y > canvas.height) particle.vy *= -1

                ctx.save()
                ctx.globalAlpha = particle.opacity
                ctx.fillStyle = particle.color
                ctx.beginPath()
                ctx.arc(particle.x, particle.y, particle.size, 0, Math.PI * 2)
                ctx.fill()
                ctx.restore()
            })

            requestAnimationFrame(animate)
        }
        animate()
    }

    const handleVideoLoad = () => {
        setVideoLoaded(true)
    }

    // Split title into characters for animation
    const title = "Sentio"
    const titleChars = title.split("")

    // Split subtitle into words for animation
    const subtitle = "Distributed environmental intelligence for climate monitoring, biodiversity conservation, and disaster prevention. Connect your backyard to global science while protecting your community through AI-powered early warning systems."
    const subtitleWords = subtitle.split(" ")

    // Floating element animation variants
    const floatingVariants = {
        animate: (i: number) => ({
            y: [0, -30 - i * 10, 0],
            x: [0, 50 + i * 20, 100 + i * 30, 0],
            boxShadow: [
                `0 0 ${20 + i * 10}px rgba(176, 214, 255, 0.${2 + i})`,
                `0 0 ${30 + i * 10}px rgba(176, 214, 255, 0.${3 + i})`,
                `0 0 ${20 + i * 10}px rgba(176, 214, 255, 0.${2 + i})`
            ],
            transition: {
                duration: 8 + i * 2,
                repeat: Infinity,
                ease: "linear" as any,
                delay: i * 0.5
            }
        })
    }

    return (
        <motion.section
            ref={heroRef}
            className="relative h-screen flex items-center justify-center overflow-hidden"
            style={{ y, opacity }}
        >
            {/* Background - only show video when loaded, canvas when error */}
            <div className="absolute inset-0">
                {!videoError && (
                    <motion.video
                        ref={videoRef}
                        className="absolute inset-0 w-full h-full object-cover"
                        style={{ filter: "blur(2px)" }}
                        initial={{ opacity: 0 }}
                        animate={{ opacity: videoLoaded ? 0.8 : 0 }}
                        transition={{ duration: 1.5, ease: "easeOut" }}
                        autoPlay
                        muted
                        loop
                        playsInline
                        onLoadedData={handleVideoLoad}
                        onError={() => setVideoError(true)}
                    >
                        <source src="/fpv_background.webm" type="video/webm" />
                    </motion.video>
                )}

                {/* Canvas Fallback - only show when video error */}
                {videoError && (
                    <canvas
                        ref={canvasRef}
                        className="absolute inset-0 w-full h-full"
                    />
                )}

                {/* Enhanced gradient overlays */}
                <div className="absolute inset-0 bg-gradient-to-br from-black/80 via-black/60 to-black/80" />
                <div className="absolute inset-0 bg-[radial-gradient(circle_at_30%_70%,rgba(176,214,255,0.15),transparent_50%)]" />
                <div className="absolute inset-0 bg-[radial-gradient(circle_at_70%_30%,rgba(168,213,186,0.1),transparent_50%)]" />
            </div>

            {/* Floating Elements - only show when video error */}
            {videoError && (
                <div className="absolute inset-0 pointer-events-none">
                    {[
                        { top: "25%", right: "25%", size: "w-32 h-32" },
                        { bottom: "33%", left: "25%", size: "w-24 h-24" },
                        { top: "50%", left: "16%", size: "w-16 h-16" },
                        { bottom: "25%", right: "33%", size: "w-20 h-20" }
                    ].map((pos, i) => (
                        <motion.div
                            key={i}
                            className={`absolute ${pos.size} rounded-full glass border border-white/20`}
                            style={{ top: pos.top, right: pos.right, bottom: pos.bottom, left: pos.left }}
                            custom={i}
                            variants={floatingVariants}
                            animate="animate"
                        />
                    ))}
                </div>
            )}

            {/* Enhanced Content */}
            <div className="relative z-10 text-center max-w-5xl mx-auto px-6">
                <h1 ref={titleRef} className="text-7xl md:text-9xl font-bold mb-8 tracking-tighter leading-none">
                    {titleChars.map((char, i) => (
                        <motion.span
                            key={i}
                            initial={{ y: 100, opacity: 0, rotateX: -90 }}
                            animate={titleInView ? { y: 0, opacity: 1, rotateX: 0 } : {}}
                            transition={{
                                duration: 1.2,
                                ease: [0.68, -0.55, 0.265, 1.55],
                                delay: 1 + i * 0.05
                            }}
                            style={{ display: "inline-block" }}
                        >
                            {char}
                        </motion.span>
                    ))}
                </h1>
                <p
                    ref={subtitleRef}
                    className="text-xl md:text-3xl text-white/90 mb-12 max-w-3xl mx-auto leading-relaxed font-light"
                >
                    {subtitleWords.map((word, i) => (
                        <motion.span
                            key={i}
                            initial={{ y: 50, opacity: 0 }}
                            animate={subtitleInView ? { y: 0, opacity: 1 } : {}}
                            transition={{
                                duration: 0.8,
                                ease: [0.25, 0.46, 0.45, 0.94],
                                delay: 1.8 + i * 0.1
                            }}
                            style={{ display: "inline-block", marginRight: "0.25em" }}
                        >
                            {word}
                        </motion.span>
                    ))}
                </p>
                <div ref={ctaRef} className="flex flex-col sm:flex-row gap-6 justify-center items-center">
                    <motion.div
                        initial={{ y: 30, opacity: 0, scale: 0.8 }}
                        animate={ctaInView ? { y: 0, opacity: 1, scale: 1 } : {}}
                        transition={{
                            duration: 0.8,
                            ease: [0.68, -0.55, 0.265, 1.55],
                            delay: 2.5
                        }}
                        whileHover={{ scale: 1.05 }}
                        whileTap={{ scale: 0.95 }}
                    >
                        <Button
                            size="lg"
                            className="bg-gradient-to-r from-[#B0D6FF] to-[#A8D5BA] text-black hover:shadow-2xl hover:shadow-blue-500/25 font-semibold px-12 py-6 text-xl rounded-2xl transition-all duration-300"
                        >
                            <Play className="w-5 h-5 mr-2" />
                            Join the Network
                        </Button>
                    </motion.div>
                    <motion.div
                        initial={{ y: 30, opacity: 0, scale: 0.8 }}
                        animate={ctaInView ? { y: 0, opacity: 1, scale: 1 } : {}}
                        transition={{
                            duration: 0.8,
                            ease: [0.68, -0.55, 0.265, 1.55],
                            delay: 2.7
                        }}
                        whileHover={{ scale: 1.05 }}
                        whileTap={{ scale: 0.95 }}
                    >
                        <Button
                            size="lg"
                            variant="outline"
                            className="border-2 border-white/40 text-white hover:bg-white/10 hover:border-white/60 px-12 py-6 text-xl bg-white/5 backdrop-blur-sm rounded-2xl transition-all duration-300"
                        >
                            Explore Solutions
                        </Button>
                    </motion.div>
                </div>
            </div>

            {/* Enhanced Scroll Indicator */}
            <div className="absolute bottom-8 left-1/2 transform -translate-x-1/2">
                <motion.div
                    animate={{ y: [0, 10, 0] }}
                    transition={{ duration: 1.5, repeat: Infinity, ease: "easeInOut" }}
                >
                    <ChevronDown className="w-8 h-8 text-white/60" />
                </motion.div>
                <div className="w-px h-20 bg-gradient-to-b from-white/60 to-transparent mx-auto mt-2" />
            </div>
        </motion.section>
    )
}
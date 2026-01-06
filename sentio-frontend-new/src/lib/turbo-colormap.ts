/**
 * Google's Turbo Colormap
 * 
 * A perceptually-uniform rainbow colormap for scientific visualization.
 * Maps values from 0-1 to RGB colors [0-255, 0-255, 0-255].
 * 
 * Reference: https://ai.googleblog.com/2019/08/turbo-improved-rainbow-colormap-for.html
 */

// Turbo colormap polynomial coefficients
const kRedVec4 = [0.13572138, 4.6153926, -42.66032258, 132.13108234];
const kRedVec2 = [-152.94239396, 59.28637943];
const kGreenVec4 = [0.09140261, 2.19418839, 4.84296658, -14.18503333];
const kGreenVec2 = [4.27729857, 2.82956604];
const kBlueVec4 = [0.1066733, 12.64194608, -60.58204836, 110.36276771];
const kBlueVec2 = [-89.90310912, 27.34824973];

function saturate(x: number): number {
    return Math.max(0, Math.min(1, x));
}

function dot4(a: number[], x: number): number {
    return a[0] + a[1] * x + a[2] * x * x + a[3] * x * x * x;
}

function dot2(a: number[], x: number): number {
    return a[0] * x * x * x * x + a[1] * x * x * x * x * x;
}

/**
 * Maps a value from 0-1 to an RGB color using the Turbo colormap.
 * 
 * @param t - Value between 0 and 1
 * @returns RGB color as [r, g, b] where each component is 0-255
 */
export function turboColormap(t: number): [number, number, number] {
    const x = saturate(t);

    const r = saturate(dot4(kRedVec4, x) + dot2(kRedVec2, x));
    const g = saturate(dot4(kGreenVec4, x) + dot2(kGreenVec2, x));
    const b = saturate(dot4(kBlueVec4, x) + dot2(kBlueVec2, x));

    return [
        Math.round(r * 255),
        Math.round(g * 255),
        Math.round(b * 255)
    ];
}

/**
 * Converts precipitation value to RGBA color for radar visualization.
 * 
 * @param precipValue - Precipitation value in units of 0.01 mm / 5 min
 * @returns RGBA color as [r, g, b, a] where each component is 0-255
 */
export function precipitationToRGBA(precipValue: number): [number, number, number, number] {
    // Normalize: treat 2.5mm in 5 minutes (value = 250) as maximum
    const normalized = Math.min(precipValue, 250) / 250;

    // Get RGB from turbo colormap
    const [r, g, b] = turboColormap(normalized);

    // Calculate alpha:
    // - No rain (0) = fully transparent
    // - Light rain = ~20% opacity
    // - Heavy rain = ~80% opacity
    const alpha = Math.max(
        Math.min(normalized * 10, 0.8) * 255,
        precipValue ? 50 : 0
    );

    return [r, g, b, Math.round(alpha)];
}

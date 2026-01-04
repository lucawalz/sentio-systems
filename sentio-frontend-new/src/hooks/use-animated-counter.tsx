import { useState, useEffect, useRef } from 'react'

interface UseAnimatedCounterOptions {
    duration?: number // in ms
    delay?: number // in ms
    decimals?: number
}

export function useAnimatedCounter(
    targetValue: number,
    options: UseAnimatedCounterOptions = {}
): number {
    const { duration = 1500, delay = 0, decimals = 0 } = options
    const [value, setValue] = useState(0)
    const startTime = useRef<number | null>(null)
    const animationFrame = useRef<number | null>(null)

    useEffect(() => {
        // Reset on target change
        setValue(0)
        startTime.current = null

        if (targetValue === 0) return

        const timeout = setTimeout(() => {
            const animate = (timestamp: number) => {
                if (startTime.current === null) {
                    startTime.current = timestamp
                }

                const elapsed = timestamp - startTime.current
                const progress = Math.min(elapsed / duration, 1)

                // Ease out cubic for smooth deceleration
                const easeOut = 1 - Math.pow(1 - progress, 3)
                const currentValue = easeOut * targetValue

                setValue(currentValue)

                if (progress < 1) {
                    animationFrame.current = requestAnimationFrame(animate)
                }
            }

            animationFrame.current = requestAnimationFrame(animate)
        }, delay)

        return () => {
            clearTimeout(timeout)
            if (animationFrame.current) {
                cancelAnimationFrame(animationFrame.current)
            }
        }
    }, [targetValue, duration, delay])

    return decimals > 0 ? parseFloat(value.toFixed(decimals)) : Math.round(value)
}

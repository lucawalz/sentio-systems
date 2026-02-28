'use client';

import { motion, type HTMLMotionProps } from 'motion/react';

import { AnimateSlot, type WithAsChild } from '@/components/ui/animate-slot';

type AnimateButtonProps = WithAsChild<
    HTMLMotionProps<'button'> & {
        hoverScale?: number;
        tapScale?: number;
    }
>;

function AnimateButton({
    hoverScale = 1.05,
    tapScale = 0.95,
    asChild = false,
    ...props
}: AnimateButtonProps) {
    const Component = asChild ? AnimateSlot : motion.button;

    return (
        <Component
            whileTap={{ scale: tapScale }}
            whileHover={{ scale: hoverScale }}
            {...props}
        />
    );
}

export { AnimateButton, type AnimateButtonProps };

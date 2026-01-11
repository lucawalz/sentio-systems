"use client";

import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import { motion } from "framer-motion";
import { Loader2 } from "lucide-react";
import type { ReactNode, ComponentProps } from "react";

export interface NativeButtonProps extends ComponentProps<typeof Button> {
  children: ReactNode;
  loading?: boolean;
  glow?: boolean;
}

const NativeButton = ({
  className,
  variant = "default",
  size = "lg",
  children,
  loading = false,
  glow = false,
  disabled,
  ...props
}: NativeButtonProps) => {
  const buttonContent = (
    <>
      {loading && <Loader2 className="w-4 h-4 mr-2 animate-spin" />}
      <motion.span
        className={cn("flex items-center gap-2")}
        animate={loading ? { opacity: [1, 0.5, 1] } : { opacity: 1 }}
        transition={
          loading
            ? { duration: 1.5, repeat: Infinity, ease: "easeInOut" }
            : { duration: 0.2 }
        }
      >
        {children}
      </motion.span>
    </>
  );

  const glassmorphismClassName = cn(
    "cursor-pointer h-10 rounded-md px-5 text-sm relative overflow-hidden",
    !glow && "shadow-md hover:shadow-lg",
    glow &&
    "shadow-lg shadow-primary/20 hover:shadow-primary/40 transition-all duration-300",
    variant === "outline" && "text-foreground/80 hover:bg-foreground/5",
    (disabled || loading) && "opacity-50 cursor-not-allowed grayscale",
    className
  );

  return (
    <motion.div
      whileHover={!disabled && !loading ? { scale: 1.02 } : {}}
      whileTap={!disabled && !loading ? { scale: 0.98 } : {}}
      transition={{ type: "spring", stiffness: 400, damping: 17 }}
      className="relative block w-fit"
    >
      {glow && !disabled && !loading && (
        <div className="absolute inset-0 rounded-full bg-primary/20 blur-xl opacity-0 hover:opacity-100 transition-opacity duration-500" />
      )}
      <Button
        variant={variant}
        size={size}
        className={glassmorphismClassName}
        disabled={disabled || loading}
        aria-busy={loading}
        {...props}
      >
        {buttonContent}
      </Button>
    </motion.div>
  );
};

export { NativeButton };

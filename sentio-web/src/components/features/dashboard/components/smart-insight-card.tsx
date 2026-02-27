"use client";

import { Card, CardContent } from "@/components/ui/card";
import { cn } from "@/lib/utils";
import { AnimatePresence, motion } from "framer-motion";
import { type ReactNode, useState } from "react";

export interface SmartInsightCardProps {
    /** Icon component to display */
    icon: ReactNode;
    /** Primary value (e.g., "12° / 5°") */
    value: string;
    /** Label below value (e.g., "Today's High/Low") */
    label: string;
    /** Trend indicator: "up" | "down" | "neutral" */
    trend?: "up" | "down" | "neutral";
    /** Trend value (e.g., "+2°") */
    trendValue?: string;
    /** Expanded content shown on hover */
    expandedContent?: ReactNode;
    /** Card variant */
    variant?: "default" | "success" | "warning" | "danger";
    /** Loading state */
    loading?: boolean;
    /** Additional class names */
    className?: string;
}

const variantStyles = {
    default: "border-border",
    success: "border-green-500/30 bg-green-500/5",
    warning: "border-yellow-500/30 bg-yellow-500/5",
    danger: "border-red-500/30 bg-red-500/5",
};

const trendColors = {
    up: "text-green-500",
    down: "text-red-500",
    neutral: "text-muted-foreground",
};

const trendIcons = {
    up: "↑",
    down: "↓",
    neutral: "→",
};

export function SmartInsightCard({
    icon,
    value,
    label,
    trend,
    trendValue,
    expandedContent,
    variant = "default",
    loading = false,
    className,
}: SmartInsightCardProps) {
    const [isHovered, setIsHovered] = useState(false);

    if (loading) {
        return (
            <Card className={cn("relative overflow-hidden", className)}>
                <CardContent className="p-4">
                    <div className="animate-pulse space-y-2">
                        <div className="h-4 w-4 bg-muted rounded" />
                        <div className="h-6 w-16 bg-muted rounded" />
                        <div className="h-3 w-20 bg-muted rounded" />
                    </div>
                </CardContent>
            </Card>
        );
    }

    return (
        <motion.div
            className="relative"
            onMouseEnter={() => setIsHovered(true)}
            onMouseLeave={() => setIsHovered(false)}
            layout
        >
            <Card
                className={cn(
                    "relative overflow-hidden transition-all duration-200 cursor-pointer",
                    variantStyles[variant],
                    isHovered && "shadow-lg ring-1 ring-primary/20",
                    className
                )}
            >
                <CardContent className="p-4">
                    {/* Main content */}
                    <div className="flex items-start justify-between">
                        <div className="space-y-1">
                            <div className="flex items-center gap-2 text-muted-foreground">
                                {icon}
                            </div>
                            <div className="text-2xl font-bold tracking-tight">{value}</div>
                            <div className="text-xs text-muted-foreground">{label}</div>
                        </div>

                        {/* Trend indicator */}
                        {trend && trendValue && (
                            <div className={cn("text-sm font-medium flex items-center gap-0.5", trendColors[trend])}>
                                <span>{trendIcons[trend]}</span>
                                <span>{trendValue}</span>
                            </div>
                        )}
                    </div>

                    {/* Expanded content */}
                    <AnimatePresence>
                        {isHovered && expandedContent && (
                            <motion.div
                                initial={{ opacity: 0, height: 0 }}
                                animate={{ opacity: 1, height: "auto" }}
                                exit={{ opacity: 0, height: 0 }}
                                transition={{ duration: 0.2 }}
                                className="overflow-hidden"
                            >
                                <div className="pt-3 mt-3 border-t border-border/50">
                                    {expandedContent}
                                </div>
                            </motion.div>
                        )}
                    </AnimatePresence>
                </CardContent>
            </Card>
        </motion.div>
    );
}

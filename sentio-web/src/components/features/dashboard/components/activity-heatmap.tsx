"use client";

import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { cn } from "@/lib/utils";
import type { AnimalDetection } from "@/types/api";

interface ActivityHeatmapProps {
    detections: AnimalDetection[];
    loading?: boolean;
    className?: string;
}

const DAYS = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"];
const HOURS = Array.from({ length: 24 }, (_, i) => i);

function getHeatmapData(detections: AnimalDetection[]) {
    // Initialize 7x24 grid with zeros
    const grid: number[][] = Array.from({ length: 7 }, () => Array(24).fill(0));

    detections.forEach((detection) => {
        const date = new Date(detection.timestamp);
        const day = date.getDay(); // 0-6
        const hour = date.getHours(); // 0-23
        grid[day][hour]++;
    });

    return grid;
}

function getIntensityColor(count: number, maxCount: number) {
    if (count === 0) return "bg-muted";
    const intensity = count / maxCount;
    if (intensity > 0.8) return "bg-green-500";
    if (intensity > 0.6) return "bg-green-400";
    if (intensity > 0.4) return "bg-green-300";
    if (intensity > 0.2) return "bg-green-200";
    return "bg-green-100";
}

export function ActivityHeatmap({
    detections,
    loading = false,
    className,
}: ActivityHeatmapProps) {
    if (loading) {
        return (
            <Card className={className}>
                <CardHeader>
                    <Skeleton className="h-5 w-40" />
                    <Skeleton className="h-4 w-60" />
                </CardHeader>
                <CardContent>
                    <Skeleton className="h-48 w-full" />
                </CardContent>
            </Card>
        );
    }

    const grid = getHeatmapData(detections);
    const maxCount = Math.max(...grid.flat(), 1);
    const totalDetections = grid.flat().reduce((a, b) => a + b, 0);

    return (
        <Card className={className}>
            <CardHeader className="pb-2">
                <CardTitle className="text-sm font-medium">Activity Heatmap</CardTitle>
                <CardDescription>
                    Detection patterns over the last 7 days ({totalDetections} total)
                </CardDescription>
            </CardHeader>
            <CardContent>
                {totalDetections === 0 ? (
                    <div className="text-center py-8 text-muted-foreground">
                        No detection data available
                    </div>
                ) : (
                    <div className="overflow-x-auto">
                        <div className="min-w-[600px]">
                            {/* Hour labels */}
                            <div className="flex mb-1 pl-10">
                                {HOURS.filter((h) => h % 3 === 0).map((hour) => (
                                    <div
                                        key={hour}
                                        className="text-xs text-muted-foreground"
                                        style={{ width: `${(100 / 8)}%` }}
                                    >
                                        {hour.toString().padStart(2, "0")}:00
                                    </div>
                                ))}
                            </div>

                            {/* Grid */}
                            {DAYS.map((day, dayIndex) => (
                                <div key={day} className="flex items-center gap-1 mb-1">
                                    <div className="w-9 text-xs text-muted-foreground text-right pr-2">
                                        {day}
                                    </div>
                                    <div className="flex-1 flex gap-[2px]">
                                        {HOURS.map((hour) => {
                                            const count = grid[dayIndex][hour];
                                            return (
                                                <div
                                                    key={hour}
                                                    className={cn(
                                                        "h-4 flex-1 rounded-sm transition-colors cursor-pointer hover:ring-1 hover:ring-primary",
                                                        getIntensityColor(count, maxCount)
                                                    )}
                                                    title={`${day} ${hour}:00 - ${count} detection${count !== 1 ? 's' : ''}`}
                                                />
                                            );
                                        })}
                                    </div>
                                </div>
                            ))}

                            {/* Legend */}
                            <div className="flex items-center justify-end gap-2 mt-3 text-xs text-muted-foreground">
                                <span>Less</span>
                                <div className="flex gap-[2px]">
                                    <div className="w-3 h-3 rounded-sm bg-muted" />
                                    <div className="w-3 h-3 rounded-sm bg-green-100" />
                                    <div className="w-3 h-3 rounded-sm bg-green-200" />
                                    <div className="w-3 h-3 rounded-sm bg-green-300" />
                                    <div className="w-3 h-3 rounded-sm bg-green-400" />
                                    <div className="w-3 h-3 rounded-sm bg-green-500" />
                                </div>
                                <span>More</span>
                            </div>
                        </div>
                    </div>
                )}
            </CardContent>
        </Card>
    );
}

"use client";

import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { cn } from "@/lib/utils";
import type { AnimalDetection } from "@/types/api";

interface SpeciesGalleryProps {
    speciesCount: Record<string, number>;
    detections: AnimalDetection[];
    loading?: boolean;
    className?: string;
}

// Map species names to emojis
export function getSpeciesEmoji(species: string): string {
    const lower = species.toLowerCase();

    // Birds
    if (lower.includes("robin")) return "🐦";
    if (lower.includes("sparrow")) return "🐦";
    if (lower.includes("finch")) return "🐦";
    if (lower.includes("blue tit") || lower.includes("bluetit")) return "🐦";
    if (lower.includes("blackbird")) return "🐦‍⬛";
    if (lower.includes("crow") || lower.includes("raven")) return "🐦‍⬛";
    if (lower.includes("magpie")) return "🐦";
    if (lower.includes("pigeon") || lower.includes("dove")) return "🕊️";
    if (lower.includes("woodpecker")) return "🪶";
    if (lower.includes("owl")) return "🦉";
    if (lower.includes("eagle") || lower.includes("hawk")) return "🦅";
    if (lower.includes("duck")) return "🦆";
    if (lower.includes("swan")) return "🦢";
    if (lower.includes("goose")) return "🦆";
    if (lower.includes("heron")) return "🦩";
    if (lower.includes("parrot")) return "🦜";
    if (lower.includes("penguin")) return "🐧";
    if (lower.includes("peacock")) return "🦚";
    if (lower.includes("flamingo")) return "🦩";
    if (lower.includes("turkey")) return "🦃";
    if (lower.includes("chicken") || lower.includes("rooster")) return "🐔";

    // Mammals
    if (lower.includes("fox")) return "🦊";
    if (lower.includes("deer") || lower.includes("doe")) return "🦌";
    if (lower.includes("rabbit") || lower.includes("bunny") || lower.includes("hare")) return "🐰";
    if (lower.includes("squirrel")) return "🐿️";
    if (lower.includes("hedgehog")) return "🦔";
    if (lower.includes("badger")) return "🦡";
    if (lower.includes("cat")) return "🐱";
    if (lower.includes("dog")) return "🐕";
    if (lower.includes("mouse") || lower.includes("rat")) return "🐭";
    if (lower.includes("bat")) return "🦇";
    if (lower.includes("bear")) return "🐻";
    if (lower.includes("wolf")) return "🐺";
    if (lower.includes("raccoon")) return "🦝";
    if (lower.includes("skunk")) return "🦨";
    if (lower.includes("beaver")) return "🦫";
    if (lower.includes("otter")) return "🦦";
    if (lower.includes("moose")) return "🫎";

    // Reptiles/Amphibians
    if (lower.includes("snake")) return "🐍";
    if (lower.includes("lizard")) return "🦎";
    if (lower.includes("frog") || lower.includes("toad")) return "🐸";
    if (lower.includes("turtle") || lower.includes("tortoise")) return "🐢";
    if (lower.includes("crocodile") || lower.includes("alligator")) return "🐊";

    // Insects
    if (lower.includes("butterfly")) return "🦋";
    if (lower.includes("bee")) return "🐝";
    if (lower.includes("ant")) return "🐜";
    if (lower.includes("spider")) return "🕷️";
    if (lower.includes("ladybug") || lower.includes("ladybird")) return "🐞";
    if (lower.includes("cricket") || lower.includes("grasshopper")) return "🦗";
    if (lower.includes("caterpillar")) return "🐛";
    if (lower.includes("snail")) return "🐌";
    if (lower.includes("worm")) return "🪱";

    // Default for unknown
    return "🐾";
}

function getLastSeen(species: string, detections: AnimalDetection[]): string | null {
    const detection = detections.find(
        (d) => d.species.toLowerCase() === species.toLowerCase()
    );
    if (!detection) return null;

    const date = new Date(detection.timestamp);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMins / 60);
    const diffDays = Math.floor(diffHours / 24);

    if (diffMins < 1) return "Just now";
    if (diffMins < 60) return `${diffMins}m ago`;
    if (diffHours < 24) return `${diffHours}h ago`;
    return `${diffDays}d ago`;
}

function getAvgConfidence(species: string, detections: AnimalDetection[]): number {
    const speciesDetections = detections.filter(
        (d) => d.species.toLowerCase() === species.toLowerCase()
    );
    if (speciesDetections.length === 0) return 0;

    const sum = speciesDetections.reduce((acc, d) => acc + d.confidence, 0);
    return sum / speciesDetections.length;
}

export function SpeciesGallery({
    speciesCount,
    detections,
    loading = false,
    className,
}: SpeciesGalleryProps) {
    if (loading) {
        return (
            <Card className={className}>
                <CardHeader>
                    <Skeleton className="h-5 w-40" />
                    <Skeleton className="h-4 w-60" />
                </CardHeader>
                <CardContent>
                    <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
                        {[1, 2, 3, 4, 5, 6].map((i) => (
                            <Skeleton key={i} className="h-24 rounded-lg" />
                        ))}
                    </div>
                </CardContent>
            </Card>
        );
    }

    const speciesList = Object.entries(speciesCount)
        .sort(([, a], [, b]) => b - a)
        .slice(0, 12);

    const totalCount = Object.values(speciesCount).reduce((a, b) => a + b, 0);

    return (
        <Card className={cn("", className)}>
            <CardHeader className="pb-2 shrink-0">
                <CardTitle className="text-sm font-medium">Species Gallery</CardTitle>
                <CardDescription>
                    {speciesList.length} species spotted ({totalCount} total sightings)
                </CardDescription>
            </CardHeader>
            <CardContent className="flex-1 overflow-hidden">
                {speciesList.length === 0 ? (
                    <div className="text-center py-8 text-muted-foreground">
                        No species detected yet
                    </div>
                ) : (
                    <div className="grid grid-cols-2 sm:grid-cols-3 gap-3 h-full overflow-y-auto pr-1">
                        {speciesList.map(([species, count]) => {
                            const lastSeen = getLastSeen(species, detections);
                            const confidence = getAvgConfidence(species, detections);

                            return (
                                <div
                                    key={species}
                                    className={cn(
                                        "relative p-3 rounded-lg border bg-card",
                                        "hover:bg-muted/50 hover:shadow-md transition-all cursor-pointer",
                                        "flex flex-col items-center text-center gap-1"
                                    )}
                                >
                                    {/* Count badge */}
                                    <Badge
                                        variant="secondary"
                                        className="absolute top-2 right-2 text-xs"
                                    >
                                        {count}
                                    </Badge>

                                    {/* Emoji */}
                                    <span className="text-3xl mb-1">{getSpeciesEmoji(species)}</span>

                                    {/* Name */}
                                    <span className="font-medium text-sm truncate w-full">{species}</span>

                                    {/* Last seen */}
                                    {lastSeen && (
                                        <span className="text-xs text-muted-foreground">{lastSeen}</span>
                                    )}

                                    {/* Confidence bar */}
                                    {confidence > 0 && (
                                        <div className="w-full mt-1">
                                            <div className="h-1 bg-muted rounded-full overflow-hidden">
                                                <div
                                                    className="h-full bg-green-500 rounded-full"
                                                    style={{ width: `${confidence * 100}%` }}
                                                />
                                            </div>
                                        </div>
                                    )}
                                </div>
                            );
                        })}
                    </div>
                )}
            </CardContent>
        </Card>
    );
}

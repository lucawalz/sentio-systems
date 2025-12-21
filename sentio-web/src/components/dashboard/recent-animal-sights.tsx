
import { Eye, Clock, PawPrint, Brain } from "lucide-react"
import { useAnimalData } from "../../hooks/useAnimalData"
import { useAnimalImages } from "../../hooks/useAnimalImages"
import { formatDistanceToNow } from "date-fns"

const getSpeciesColor = (species: string): string => {
    const colors = [
        "#10B981", "#3B82F6", "#F59E0B", "#059669", "#DC2626",
        "#34D399", "#60A5FA", "#FBBF24", "#16A34A", "#EF4444"
    ];

    let hash = 0;
    for (let i = 0; i < species.length; i++) {
        hash = species.charCodeAt(i) + ((hash << 5) - hash);
    }
    return colors[Math.abs(hash) % colors.length];
};

const AnimalImage = ({ species, color }: { species: string; color: string }) => {
    const { getAnimalImage, isLoadingImage } = useAnimalImages([species]);
    const imageUrl = getAnimalImage(species);
    const loading = isLoadingImage(species);

    if (loading) {
        return (
            <div className="w-10 h-10 rounded-xl bg-secondary/40 animate-pulse flex items-center justify-center border border-border/20">
                <div className="w-5 h-5 bg-secondary/60 rounded-full animate-pulse" />
            </div>
        );
    }

    if (imageUrl) {
        return (
            <div className="w-10 h-10 rounded-xl overflow-hidden border border-border/30 shadow-md">
                <img
                    src={imageUrl}
                    alt={species}
                    className="w-full h-full object-cover"
                    onError={(e) => {
                        e.currentTarget.style.display = 'none';
                        e.currentTarget.nextElementSibling?.classList.remove('hidden');
                    }}
                />
                <div
                    className="hidden w-full h-full flex items-center justify-center rounded-xl"
                    style={{ backgroundColor: color }}
                >
                    <PawPrint className="w-4 h-4 text-white" />
                </div>
            </div>
        );
    }

    return (
        <div
            className="w-10 h-10 rounded-xl flex items-center justify-center border border-border/30 shadow-md"
            style={{ backgroundColor: color }}
        >
            <PawPrint className="w-4 h-4 text-white" />
        </div>
    );
};

// Enhanced Confidence Bar
const ConfidenceBar = ({ confidence }: { confidence: number }) => {
    const percentage = Math.round(confidence * 100);

    // Generate lighter version of the same color for gradients
    const getConfidenceColorGradient = (conf: number) => {
        let baseColor: string;

        if (conf >= 0.8) {
            baseColor = "#10B981"; // success green
        } else if (conf >= 0.6) {
            baseColor = "#F59E0B"; // warning amber
        } else {
            baseColor = "#DC2626"; // destructive red
        }

        // Convert hex to RGB
        const hex = baseColor.replace('#', '');
        const r = parseInt(hex.substr(0, 2), 16);
        const g = parseInt(hex.substr(2, 2), 16);
        const b = parseInt(hex.substr(4, 2), 16);

        // Create lighter version (mix with white, but not completely)
        const lightR = Math.round(r + (255 - r) * 0.6);
        const lightG = Math.round(g + (255 - g) * 0.6);
        const lightB = Math.round(b + (255 - b) * 0.6);

        return {
            from: baseColor,
            to: `rgb(${lightR}, ${lightG}, ${lightB})`
        };
    };

    const gradient = getConfidenceColorGradient(confidence);

    return (
        <div className="flex items-center space-x-2">
            <div className="flex-1 h-1.5 bg-secondary rounded-full overflow-hidden">
                <div
                    className="h-full"
                    style={{
                        width: `${percentage}%`,
                        background: `linear-gradient(to right, ${gradient.from}, ${gradient.to})`
                    }}
                />
            </div>
            <span className="text-xs font-medium text-muted-foreground min-w-[30px]">
                {percentage}%
            </span>
        </div>
    );
};

export function RecentAnimalSights() {
    const { latestDetections, loading, error } = useAnimalData();

    if (loading) {
        return (
            <div className="dashboard-card bg-card/80 backdrop-blur-sm border border-border rounded-3xl p-6 md:p-8 h-[696.48px] min-h-[400px] relative overflow-hidden shadow-xl">
                <div className="flex items-center justify-center h-full flex-col space-y-4">
                    <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary"></div>
                    <div className="text-sm text-muted-foreground">Loading animal sightings...</div>
                </div>
            </div>
        );
    }

    if (error) {
        return (
            <div className="dashboard-card bg-card/80 backdrop-blur-sm border border-border rounded-3xl p-6 md:p-8 h-[696.48px] min-h-[400px] relative overflow-hidden shadow-xl">
                <div className="flex flex-col items-center justify-center h-full text-center">
                    <Eye className="w-16 h-16 text-destructive mb-4" />
                    <div className="text-xl font-semibold mb-2 text-destructive">Connection Error</div>
                    <div className="text-sm text-muted-foreground mb-6 max-w-md">Error loading animal sightings</div>
                </div>
            </div>
        );
    }

    if (!latestDetections.length) {
        return (
            <div className="dashboard-card bg-card/80 backdrop-blur-sm border border-border rounded-3xl p-6 md:p-8 h-[696.48px] min-h-[400px] relative overflow-hidden shadow-xl">
                <div className="flex flex-col items-center justify-center h-full text-center">
                    <PawPrint className="w-16 h-16 text-muted-foreground mb-4" />
                    <div className="text-xl font-semibold mb-2 text-foreground">No Recent Animal Sightings</div>
                    <div className="text-sm text-muted-foreground mb-6 max-w-md">
                        No animal sightings have been detected yet. The system is ready to capture animal activity.
                    </div>
                </div>
            </div>
        );
    }

    return (
        <div className="dashboard-card bg-card/80 backdrop-blur-sm border border-border rounded-3xl p-6 md:p-8 h-[712.48px] min-h-[400px] relative overflow-hidden hover:shadow-lg transition-shadow duration-300">
            {/* Background gradient */}
            <div className="absolute inset-0 bg-gradient-to-br from-primary/5 via-transparent to-chart-2/5 pointer-events-none rounded-3xl" />

            <div className="relative z-10 h-full flex flex-col">
                {/* Header */}
                <div className="flex items-start justify-between mb-8 flex-shrink-0">
                    <div className="flex items-center space-x-3">
                        <Eye className="w-6 h-6 text-primary" />
                        <h2 className="text-2xl md:text-3xl font-bold text-foreground">
                            Recent Animals
                        </h2>
                    </div>
                    <div className="flex items-center space-x-2 text-xs text-muted-foreground">
                        <div className="w-2 h-2 bg-green-500 rounded-full animate-pulse" />
                        <span>Live Detection</span>
                    </div>
                </div>

                {/* Animal Sightings List */}
                <div className="flex-1 space-y-2 overflow-y-auto custom-scrollbar min-h-0">
                    {latestDetections.slice(0, 8).map((detection, index) => {
                        const species = detection.species || "Unknown";
                        const confidence = detection.confidence;
                        const color = getSpeciesColor(species);
                        const timeAgo = formatDistanceToNow(new Date(detection.timestamp), { addSuffix: true });

                        return (
                            <div
                                key={detection.id || index}
                                className="bg-card/60 backdrop-blur-sm rounded-xl border border-border/50 hover:border-primary/30 transition-colors duration-200 p-3 flex-shrink-0"
                            >
                                <div className="flex items-center space-x-3">
                                    <AnimalImage species={species} color={color} />

                                    <div className="flex-1 min-w-0">
                                        <div className="flex items-center space-x-2 mb-1">
                                            <h4 className="font-medium text-foreground truncate text-sm">{species}</h4>
                                            {detection.animalType && (
                                                <div className="flex items-center space-x-1">
                                                    <Brain className="w-3 h-3 text-primary" />
                                                    <span className="text-xs text-primary font-medium uppercase">{detection.animalType}</span>
                                                </div>
                                            )}
                                        </div>

                                        <div className="mb-1">
                                            <div className="flex items-center justify-between mb-0.5">
                                                <span className="text-xs text-muted-foreground font-medium">
                                                    Confidence
                                                </span>
                                            </div>
                                            <ConfidenceBar confidence={confidence} />
                                        </div>

                                        <div className="flex items-center space-x-1 text-xs text-muted-foreground">
                                            <Clock className="w-3 h-3" />
                                            <span>{timeAgo}</span>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        );
                    })}
                </div>

                {/* Footer Info */}
                <div className="pt-4 border-t border-border/20 flex-shrink-0">
                    <div className="text-xs text-muted-foreground opacity-60 text-center">
                        Real-time animal detection and identification
                    </div>
                </div>
            </div>
        </div>
    );
}
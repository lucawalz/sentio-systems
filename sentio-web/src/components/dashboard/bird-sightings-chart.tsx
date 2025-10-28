
import { BarChart, Bar, XAxis, YAxis, ResponsiveContainer, Cell } from "recharts"
import { useBirdData } from "../../hooks/useBirdData"
import { useMemo } from "react"
import { BarChart3, Bird, TrendingUp } from "lucide-react"

// Color palette for species-based gradients
const getSpeciesColors = (species: string): { from: string; to: string } => {
    const colorPalettes = [
        { from: "#10B981", to: "#34D399" }, // Green
        { from: "#3B82F6", to: "#60A5FA" }, // Blue
        { from: "#F59E0B", to: "#FBBF24" }, // Amber
        { from: "#059669", to: "#10B981" }, // Emerald
        { from: "#DC2626", to: "#EF4444" }, // Red
        { from: "#8B5CF6", to: "#A78BFA" }, // Purple
        { from: "#EC4899", to: "#F472B6" }, // Pink
        { from: "#06B6D4", to: "#22D3EE" }, // Cyan
        { from: "#84CC16", to: "#A3E635" }, // Lime
        { from: "#F97316", to: "#FB923C" }, // Orange
    ];

    let hash = 0;
    for (let i = 0; i < species.length; i++) {
        hash = species.charCodeAt(i) + ((hash << 5) - hash);
    }
    return colorPalettes[Math.abs(hash) % colorPalettes.length];
};

export function BirdSightingsChart() {
    const { latestDetections, loading, error } = useBirdData();

    // Process the data to count species using speciesAiClassified with fallback to species
    const chartData = useMemo(() => {
        if (!latestDetections.length) return [];

        const speciesCount = latestDetections.reduce((acc, detection) => {
            const species = detection.speciesAiClassified || detection.species || "Unknown";
            acc[species] = (acc[species] || 0) + 1;
            return acc;
        }, {} as Record<string, number>);

        // Convert to chart format and sort by count (descending)
        return Object.entries(speciesCount)
            .map(([species, count]) => ({ species, count }))
            .sort((a, b) => b.count - a.count)
            .slice(0, 10); // Limit to top 10 species
    }, [latestDetections]);

    // Calculate stats
    const totalSightings = latestDetections.length;
    const uniqueSpecies = chartData.length;

    if (loading) {
        return (
            <div className="dashboard-card bg-card/80 backdrop-blur-sm border border-border rounded-3xl p-6 md:p-8 h-[696.48px] min-h-[400px] relative overflow-hidden shadow-xl">
                <div className="flex items-center justify-center h-full flex-col space-y-4">
                    <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary"></div>
                    <div className="text-sm text-muted-foreground">Loading bird sightings chart...</div>
                </div>
            </div>
        );
    }

    if (error) {
        return (
            <div className="dashboard-card bg-card/80 backdrop-blur-sm border border-border rounded-3xl p-6 md:p-8 h-[696.48px] min-h-[400px] relative overflow-hidden shadow-xl">
                <div className="flex flex-col items-center justify-center h-full text-center">
                    <BarChart3 className="w-16 h-16 text-destructive mb-4" />
                    <div className="text-xl font-semibold mb-2 text-destructive">Connection Error</div>
                    <div className="text-sm text-muted-foreground mb-6 max-w-md">Error loading chart data</div>
                </div>
            </div>
        );
    }

    if (!chartData.length) {
        return (
            <div className="dashboard-card bg-card/80 backdrop-blur-sm border border-border rounded-3xl p-6 md:p-8 h-[696.48px] min-h-[400px] relative overflow-hidden shadow-xl">
                <div className="flex flex-col items-center justify-center h-full text-center">
                    <Bird className="w-16 h-16 text-muted-foreground mb-4" />
                    <div className="text-xl font-semibold mb-2 text-foreground">No Bird Sightings Data</div>
                    <div className="text-sm text-muted-foreground mb-6 max-w-md">
                        No bird sightings data available to display in the chart.
                    </div>
                </div>
            </div>
        );
    }

    return (
        <div className="dashboard-card bg-card/80 backdrop-blur-sm border border-border rounded-3xl p-6 md:p-8 h-[696.48px] min-h-[400px] relative overflow-hidden hover:shadow-lg transition-shadow duration-300">
            {/* Background gradient */}
            <div className="absolute inset-0 bg-gradient-to-br from-primary/5 via-transparent to-chart-2/5 pointer-events-none rounded-3xl" />

            <div className="relative z-10 h-full flex flex-col">
                {/* Header */}
                <div className="flex items-start justify-between mb-8 flex-shrink-0">
                    <div className="flex items-center space-x-3">
                        <BarChart3 className="w-6 h-6 text-primary" />
                        <h2 className="text-2xl md:text-3xl font-bold text-foreground">
                            Bird Sightings Chart
                        </h2>
                    </div>
                    <div className="flex items-center space-x-2 text-xs text-muted-foreground">
                        <div className="w-2 h-2 bg-green-500 rounded-full animate-pulse" />
                        <span>Live Data</span>
                    </div>
                </div>

                {/* Stats Cards */}
                <div className="flex space-x-4 mb-6 flex-shrink-0">
                    <div className="bg-card/60 backdrop-blur-sm rounded-xl border border-border/50 p-4 flex-1">
                        <div className="flex items-center space-x-2 mb-1">
                            <TrendingUp className="w-4 h-4 text-primary" />
                            <span className="text-sm font-medium text-muted-foreground">Total Sightings</span>
                        </div>
                        <div className="text-2xl font-bold text-foreground">{totalSightings}</div>
                    </div>
                    <div className="bg-card/60 backdrop-blur-sm rounded-xl border border-border/50 p-4 flex-1">
                        <div className="flex items-center space-x-2 mb-1">
                            <Bird className="w-4 h-4 text-primary" />
                            <span className="text-sm font-medium text-muted-foreground">Unique Species</span>
                        </div>
                        <div className="text-2xl font-bold text-foreground">{uniqueSpecies}</div>
                    </div>
                </div>

                {/* Chart */}
                <div className="flex-1 min-h-0">
                    <ResponsiveContainer width="100%" height="100%">
                        <BarChart data={chartData} margin={{ top: 20, right: 30, left: 20, bottom: 80 }}>
                            <XAxis
                                dataKey="species"
                                axisLine={false}
                                tickLine={false}
                                tick={{ fontSize: 12, fill: "var(--muted-foreground)" }}
                                angle={-45}
                                textAnchor="end"
                                height={80}
                            />
                            <YAxis
                                axisLine={false}
                                tickLine={false}
                                tick={{ fontSize: 12, fill: "var(--muted-foreground)" }}
                            />
                            <Bar dataKey="count" radius={[8, 8, 0, 0]}>
                                {chartData.map((entry, index) => (
                                    <Cell key={`cell-${index}`} fill={`url(#gradient-${entry.species.replace(/[^a-zA-Z0-9]/g, '')})`} />
                                ))}
                            </Bar>
                            <defs>
                                {chartData.map((entry) => {
                                    const colors = getSpeciesColors(entry.species);
                                    const gradientId = `gradient-${entry.species.replace(/[^a-zA-Z0-9]/g, '')}`;
                                    return (
                                        <linearGradient key={gradientId} id={gradientId} x1="0" y1="0" x2="0" y2="1">
                                            <stop offset="0%" stopColor={colors.from} />
                                            <stop offset="100%" stopColor={colors.to} />
                                        </linearGradient>
                                    );
                                })}
                            </defs>
                        </BarChart>
                    </ResponsiveContainer>
                </div>

                {/* Footer Info */}
                <div className="pt-4 border-t border-border/20 flex-shrink-0">
                    <div className="text-xs text-muted-foreground opacity-60 text-center">
                        Top 10 most frequently detected bird species
                    </div>
                </div>
            </div>
        </div>
    )
}
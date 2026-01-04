import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip } from 'recharts'

interface SpeciesChartProps {
    speciesBreakdown: Record<string, number>
    animalTypeBreakdown: Record<string, number>
    loading: boolean
}

const COLORS = [
    '#10b981', // emerald
    '#3b82f6', // blue
    '#f59e0b', // amber
    '#ef4444', // red
    '#8b5cf6', // violet
    '#ec4899', // pink
    '#06b6d4', // cyan
    '#84cc16', // lime
]

export function SpeciesChart({ speciesBreakdown, animalTypeBreakdown, loading }: SpeciesChartProps) {
    // Prepare data for species (top 6)
    const speciesData = Object.entries(speciesBreakdown || {})
        .sort(([, a], [, b]) => b - a)
        .slice(0, 6)
        .map(([name, value]) => ({ name, value }))

    // Prepare data for animal type
    const typeData = Object.entries(animalTypeBreakdown || {})
        .map(([name, value]) => ({
            name: name.charAt(0).toUpperCase() + name.slice(1),
            value
        }))

    const hasData = speciesData.length > 0 || typeData.length > 0

    return (
        <Card>
            <CardHeader className="pb-2">
                <CardTitle className="text-sm font-medium">Species Distribution</CardTitle>
            </CardHeader>
            <CardContent>
                {loading ? (
                    <div className="flex items-center justify-center h-32">
                        <Skeleton className="h-24 w-24 rounded-full" />
                    </div>
                ) : !hasData ? (
                    <div className="flex items-center justify-center h-24 text-muted-foreground text-sm">
                        No detection data yet
                    </div>
                ) : (
                    <div className="grid grid-cols-2 gap-4">
                        {/* Type Distribution (Bird vs Mammal) */}
                        {typeData.length > 0 && (
                            <div>
                                <p className="text-xs text-muted-foreground text-center mb-2">By Type</p>
                                <ResponsiveContainer width="100%" height={80}>
                                    <PieChart>
                                        <Pie
                                            data={typeData}
                                            cx="50%"
                                            cy="50%"
                                            innerRadius={25}
                                            outerRadius={45}
                                            paddingAngle={2}
                                            dataKey="value"
                                        >
                                            {typeData.map((_, index) => (
                                                <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                                            ))}
                                        </Pie>
                                        <Tooltip
                                            contentStyle={{
                                                backgroundColor: 'hsl(var(--background))',
                                                border: '1px solid hsl(var(--border))',
                                                borderRadius: '8px'
                                            }}
                                        />
                                    </PieChart>
                                </ResponsiveContainer>
                                <div className="flex justify-center gap-3 mt-1">
                                    {typeData.map((entry, index) => (
                                        <div key={entry.name} className="flex items-center gap-1 text-xs">
                                            <div
                                                className="w-2 h-2 rounded-full"
                                                style={{ backgroundColor: COLORS[index % COLORS.length] }}
                                            />
                                            <span>{entry.name}</span>
                                        </div>
                                    ))}
                                </div>
                            </div>
                        )}

                        {/* Species Distribution */}
                        {speciesData.length > 0 && (
                            <div>
                                <p className="text-xs text-muted-foreground text-center mb-2">Top Species</p>
                                <ResponsiveContainer width="100%" height={80}>
                                    <PieChart>
                                        <Pie
                                            data={speciesData}
                                            cx="50%"
                                            cy="50%"
                                            innerRadius={25}
                                            outerRadius={45}
                                            paddingAngle={2}
                                            dataKey="value"
                                        >
                                            {speciesData.map((_, index) => (
                                                <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                                            ))}
                                        </Pie>
                                        <Tooltip
                                            contentStyle={{
                                                backgroundColor: 'hsl(var(--background))',
                                                border: '1px solid hsl(var(--border))',
                                                borderRadius: '8px'
                                            }}
                                        />
                                    </PieChart>
                                </ResponsiveContainer>
                                <div className="flex flex-wrap justify-center gap-x-3 gap-y-1 mt-1">
                                    {speciesData.slice(0, 4).map((entry, index) => (
                                        <div key={entry.name} className="flex items-center gap-1 text-xs">
                                            <div
                                                className="w-2 h-2 rounded-full"
                                                style={{ backgroundColor: COLORS[index % COLORS.length] }}
                                            />
                                            <span className="truncate max-w-[60px]">{entry.name}</span>
                                        </div>
                                    ))}
                                </div>
                            </div>
                        )}
                    </div>
                )}
            </CardContent>
        </Card>
    )
}

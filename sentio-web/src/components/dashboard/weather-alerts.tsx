import { useEffect, useRef, useState } from "react"
import { gsap } from "gsap"
import {
    AlertTriangle,
    AlertCircle,
    Info,
    ShieldAlert,
    ChevronDown,
    ChevronUp,
    Clock,
    MapPin,
    RefreshCw,
    WifiOff,
    Bell,
    BellOff
} from "lucide-react"
import { useAlerts } from "../../hooks/useAlerts"
import type { WeatherAlert } from "../../services/alertService"

// Severity color mapping
const getSeverityConfig = (severity: string | null) => {
    switch (severity?.toLowerCase()) {
        case 'extreme':
            return {
                color: 'from-purple-500 to-purple-700',
                bgColor: 'bg-purple-500/10',
                borderColor: 'border-purple-500/30',
                textColor: 'text-purple-400',
                icon: ShieldAlert,
                label: 'Extreme'
            };
        case 'severe':
            return {
                color: 'from-red-500 to-red-700',
                bgColor: 'bg-red-500/10',
                borderColor: 'border-red-500/30',
                textColor: 'text-red-400',
                icon: AlertTriangle,
                label: 'Severe'
            };
        case 'moderate':
            return {
                color: 'from-orange-500 to-orange-700',
                bgColor: 'bg-orange-500/10',
                borderColor: 'border-orange-500/30',
                textColor: 'text-orange-400',
                icon: AlertCircle,
                label: 'Moderate'
            };
        case 'minor':
        default:
            return {
                color: 'from-yellow-500 to-yellow-700',
                bgColor: 'bg-yellow-500/10',
                borderColor: 'border-yellow-500/30',
                textColor: 'text-yellow-400',
                icon: Info,
                label: 'Minor'
            };
    }
};

const formatAlertTime = (dateStr: string) => {
    if (!dateStr) return 'Unknown';
    const date = new Date(dateStr);
    return date.toLocaleString('en-US', {
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    });
};

const AlertCard = ({ alert, isExpanded, onToggle }: {
    alert: WeatherAlert;
    isExpanded: boolean;
    onToggle: () => void;
}) => {
    const config = getSeverityConfig(alert.severity);
    const Icon = config.icon;

    // Use localized fields or fall back to English
    const headline = alert.localizedHeadline || alert.headlineEn || 'Weather Alert';
    const description = alert.localizedDescription || alert.descriptionEn || '';
    const event = alert.localizedEvent || alert.eventEn || '';
    const instruction = alert.localizedInstruction || alert.instructionEn || '';

    return (
        <div
            className={`${config.bgColor} ${config.borderColor} border rounded-xl p-4 transition-all duration-300 hover:shadow-md cursor-pointer`}
            onClick={onToggle}
        >
            <div className="flex items-start justify-between gap-3">
                <div className={`w-10 h-10 rounded-lg bg-gradient-to-br ${config.color} flex items-center justify-center flex-shrink-0`}>
                    <Icon className="w-5 h-5 text-white" />
                </div>
                <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 mb-1">
                        <span className={`text-xs font-semibold uppercase tracking-wide ${config.textColor}`}>
                            {config.label}
                        </span>
                        {event && (
                            <span className="text-xs text-muted-foreground">• {event}</span>
                        )}
                    </div>
                    <h4 className="text-sm font-medium text-foreground line-clamp-2">
                        {headline}
                    </h4>
                    <div className="flex items-center gap-2 mt-2 text-xs text-muted-foreground">
                        <Clock className="w-3 h-3" />
                        <span>{formatAlertTime(alert.onset)}</span>
                        {alert.city && (
                            <>
                                <MapPin className="w-3 h-3 ml-2" />
                                <span>{alert.city}</span>
                            </>
                        )}
                    </div>
                </div>
                <button className="p-1 hover:bg-white/10 rounded transition-colors">
                    {isExpanded ? (
                        <ChevronUp className="w-4 h-4 text-muted-foreground" />
                    ) : (
                        <ChevronDown className="w-4 h-4 text-muted-foreground" />
                    )}
                </button>
            </div>

            {isExpanded && (
                <div className="mt-4 pt-4 border-t border-border/50 space-y-3">
                    <p className="text-sm text-muted-foreground leading-relaxed">
                        {description}
                    </p>
                    {instruction && (
                        <div className="bg-card/50 rounded-lg p-3">
                            <p className="text-xs font-medium text-foreground mb-1">Instructions:</p>
                            <p className="text-xs text-muted-foreground">{instruction}</p>
                        </div>
                    )}
                    {alert.expires && (
                        <p className="text-xs text-muted-foreground">
                            Expires: {formatAlertTime(alert.expires)}
                        </p>
                    )}
                </div>
            )}
        </div>
    );
};

export function WeatherAlerts() {
    const cardRef = useRef<HTMLDivElement>(null)
    const { alerts, loading, error, refetch, hasAlerts } = useAlerts(300000) // 5 min refresh
    const [expandedAlertId, setExpandedAlertId] = useState<string | null>(null)

    useEffect(() => {
        if (cardRef.current) {
            gsap.fromTo(
                cardRef.current,
                { y: 30, opacity: 0, scale: 0.95 },
                { y: 0, opacity: 1, scale: 1, duration: 1.2, ease: "power3.out", delay: 0.3 }
            )
        }
    }, [])

    if (loading) {
        return (
            <div className="dashboard-card bg-card/80 backdrop-blur-sm border border-border rounded-3xl p-6 min-h-[300px] relative overflow-hidden shadow-xl">
                <div className="flex items-center justify-center h-full flex-col space-y-4">
                    <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary"></div>
                    <div className="text-sm text-muted-foreground">Loading weather alerts...</div>
                </div>
            </div>
        )
    }

    if (error) {
        return (
            <div className="dashboard-card bg-card/80 backdrop-blur-sm border border-border rounded-3xl p-6 min-h-[300px] relative overflow-hidden shadow-xl">
                <div className="flex flex-col items-center justify-center h-full text-center">
                    <WifiOff className="w-12 h-12 text-destructive mb-4" />
                    <div className="text-lg font-semibold mb-2 text-destructive">Connection Error</div>
                    <div className="text-sm text-muted-foreground mb-4 max-w-md">{error}</div>
                    <button
                        onClick={refetch}
                        className="px-4 py-2 bg-destructive text-destructive-foreground rounded-lg hover:bg-destructive/90 transition-colors flex items-center gap-2"
                    >
                        <RefreshCw className="w-4 h-4" />
                        Retry
                    </button>
                </div>
            </div>
        )
    }

    return (
        <div
            ref={cardRef}
            className="dashboard-card bg-card/80 backdrop-blur-sm border border-border rounded-3xl p-6 min-h-[300px] relative overflow-hidden hover:shadow-lg transition-shadow duration-300"
        >
            {/* Background gradient */}
            <div className="absolute inset-0 bg-gradient-to-br from-amber-500/5 via-transparent to-red-500/5 pointer-events-none rounded-3xl" />

            <div className="relative z-10">
                {/* Header */}
                <div className="flex items-center justify-between mb-6">
                    <div className="flex items-center space-x-3">
                        {hasAlerts ? (
                            <Bell className="w-6 h-6 text-amber-500" />
                        ) : (
                            <BellOff className="w-6 h-6 text-muted-foreground" />
                        )}
                        <h3 className="text-xl font-bold text-foreground">Weather Alerts</h3>
                        {hasAlerts && (
                            <span className="px-2 py-0.5 bg-amber-500/20 text-amber-500 text-xs font-medium rounded-full">
                                {alerts.length} active
                            </span>
                        )}
                    </div>
                    <button
                        onClick={refetch}
                        className="p-2 hover:bg-card rounded-lg transition-colors"
                        title="Refresh alerts"
                    >
                        <RefreshCw className="w-4 h-4 text-muted-foreground" />
                    </button>
                </div>

                {/* Alerts List */}
                {hasAlerts ? (
                    <div className="space-y-3 max-h-[400px] overflow-y-auto pr-2">
                        {alerts.map((alert) => (
                            <AlertCard
                                key={alert.alertId}
                                alert={alert}
                                isExpanded={expandedAlertId === alert.alertId}
                                onToggle={() => setExpandedAlertId(
                                    expandedAlertId === alert.alertId ? null : alert.alertId
                                )}
                            />
                        ))}
                    </div>
                ) : (
                    <div className="flex flex-col items-center justify-center py-12 text-center">
                        <div className="w-16 h-16 rounded-full bg-green-500/10 flex items-center justify-center mb-4">
                            <BellOff className="w-8 h-8 text-green-500" />
                        </div>
                        <div className="text-lg font-medium text-foreground mb-1">No Active Alerts</div>
                        <p className="text-sm text-muted-foreground max-w-xs">
                            There are currently no weather alerts for your location. Stay safe!
                        </p>
                    </div>
                )}

                {/* Footer */}
                <div className="mt-4 pt-4 border-t border-border/50 text-center">
                    <p className="text-xs text-muted-foreground">
                        Powered by BrightSky • DWD Weather Alerts
                    </p>
                </div>
            </div>
        </div>
    )
}

import { useEffect, useRef, useState } from "react"
import { gsap } from "gsap"
import { Brain, Sparkles, Target, Clock, Users, ChevronDown, ChevronUp } from "lucide-react"
import { AiSummaryService, type AISummary } from "../../services/aiSummaryService"

export function EnhancedAISummary() {
    const cardRef = useRef<HTMLDivElement>(null)
    const iconRef = useRef<HTMLDivElement>(null)
    const [aiSummary, setAiSummary] = useState<AISummary | null>(null)
    const [loading, setLoading] = useState(true)
    const [isExpanded, setIsExpanded] = useState(false)

    useEffect(() => {
        gsap.fromTo(
            cardRef.current,
            { y: 30, opacity: 0, scale: 0.95 },
            { y: 0, opacity: 1, scale: 1, duration: 1.2, ease: "power3.out", delay: 0.2 },
        )

        // Pulsing AI icon
        gsap.to(iconRef.current, {
            scale: 1.1,
            duration: 2,
            ease: "power2.inOut",
            yoyo: true,
            repeat: -1,
        })
    }, [])

    useEffect(() => {
        const fetchAiSummary = async () => {
            setLoading(true)
            try {
                const summary = await AiSummaryService.getCurrentSummary()
                setAiSummary(summary)
            } catch (error) {
                console.error('Failed to fetch AI summary:', error)
            } finally {
                setLoading(false)
            }
        }

        fetchAiSummary()

        // Refresh every 5 minutes
        const interval = setInterval(fetchAiSummary, 5 * 60 * 1000)
        return () => clearInterval(interval)
    }, [])

    // Helper function to format time
    const formatTime = (timeString: string | null | undefined) => {
        if (!timeString) return "TBD"
        try {
            // Handle different time formats from backend
            if (timeString.includes(':')) {
                return timeString.substring(0, 5) // Extract HH:MM
            }
            return timeString
        } catch {
            return "TBD"
        }
    }

    // Helper function to get display text with expand/collapse functionality
    const getDisplayText = () => {
        if (!aiSummary?.analysisText) {
            return loading ? "Loading AI insights..." : "No AI summary available yet."
        }

        const text = aiSummary.analysisText

        // Convert ## headings to proper markdown formatting
        const formattedText = text.replace(/^##\s*(.+)$/gm, '**$1**').trim()

        // If expanded, show full text
        if (isExpanded) {
            return formattedText
        }

        // Reduced truncation to make component shorter - show up to 600 characters
        const generousLimit = 600
        if (formattedText.length <= generousLimit) {
            return formattedText
        }

        // Find the last complete sentence within the limit
        const truncated = formattedText.substring(0, generousLimit)
        const lastPeriod = truncated.lastIndexOf('.')
        const lastExclamation = truncated.lastIndexOf('!')
        const lastQuestion = truncated.lastIndexOf('?')

        const lastSentenceEnd = Math.max(lastPeriod, lastExclamation, lastQuestion)

        // More lenient threshold for sentence breaks
        if (lastSentenceEnd > 350) {
            return truncated.substring(0, lastSentenceEnd + 1)
        }

        // If no good sentence break found, show the truncated text with ellipsis
        return truncated + "..."
    }

    const canExpand = aiSummary?.analysisText && aiSummary.analysisText.length > 600

    return (
        <div
            ref={cardRef}
            className="dashboard-card bg-card/80 backdrop-blur-sm border border-border rounded-3xl p-4 md:p-5 h-full relative overflow-hidden hover:shadow-xl transition-shadow duration-300"
        >
        <div className="absolute inset-0 bg-gradient-to-br from-[#B5FFE9]/5 to-[#87B3FF]/5 pointer-events-none" />

            <div className="relative z-10 h-full flex flex-col">
                <div className="flex items-center space-x-3 mb-3">
                    <div
                        ref={iconRef}
                        className="w-9 h-9 rounded-xl bg-gradient-to-br from-[#B5FFE9] to-[#87B3FF] flex items-center justify-center shadow-lg"
                    >
                        <Brain className="w-4 h-4 text-white" />
                    </div>
                    <div>
                        <h3 className="text-base font-bold text-foreground">AI Summary</h3>
                        <div className="flex items-center space-x-1">
                            <Sparkles className="w-3 h-3 text-primary/60" />
                            <span className="text-xs text-muted-foreground font-medium">
                            {loading ? "Loading..." : "AI-Powered Insights"}
                          </span>
                        </div>
                    </div>
                </div>

                <div className="flex-1 flex flex-col justify-between">
                    <div className="mb-3">
                        <div className={`text-sm text-foreground/80 leading-relaxed prose prose-sm max-w-none prose-headings:text-foreground prose-strong:text-foreground prose-strong:font-bold ${isExpanded ? 'max-h-none' : ''}`}>
                            {getDisplayText().split('\n').map((line, index) => (
                                <p key={index} className="mb-2 last:mb-0" dangerouslySetInnerHTML={{
                                    __html: line.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
                                }} />
                            ))}
                        </div>
                        {canExpand && (
                            <button
                                onClick={() => setIsExpanded(!isExpanded)}
                                className="mt-2 flex items-center space-x-1 text-xs text-primary hover:text-primary/80 transition-colors"
                            >
                                {isExpanded ? (
                                    <>
                                        <ChevronUp className="w-3 h-3" />
                                        <span>Show less</span>
                                    </>
                                ) : (
                                    <>
                                        <ChevronDown className="w-3 h-3" />
                                        <span>Show more</span>
                                    </>
                                )}
                            </button>
                        )}
                    </div>

                    <div className="grid grid-cols-3 gap-2">
                        <div className="text-center">
                            <div className="flex items-center justify-center space-x-1 mb-1">
                                <Target className="w-3 h-3 text-[#87B3FF]" />
                                <div className="text-base font-bold text-[#87B3FF]">
                                    {aiSummary?.accuracyPercentage
                                        ? `${Math.round(aiSummary.accuracyPercentage)}%`
                                        : loading ? "..." : "N/A"}
                                </div>
                            </div>
                            <div className="text-xs text-muted-foreground">Accuracy</div>
                        </div>
                        <div className="text-center">
                            <div className="flex items-center justify-center space-x-1 mb-1">
                                <Users className="w-3 h-3 text-primary" />
                                <div className="text-base font-bold text-primary">
                                    {aiSummary?.expectedSpecies || (loading ? "..." : "N/A")}
                                </div>
                            </div>
                            <div className="text-xs text-muted-foreground">Species</div>
                        </div>
                        <div className="text-center">
                            <div className="flex items-center justify-center space-x-1 mb-1">
                                <Clock className="w-3 h-3 text-[#FFD8A8]" />
                                <div className="text-base font-bold text-[#FFD8A8]">
                                    {formatTime(aiSummary?.peakActivityTime)}
                                </div>
                            </div>
                            <div className="text-xs text-muted-foreground">Peak Time</div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    )
}
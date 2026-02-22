import { useState, useEffect, useRef } from 'react'
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { Badge } from '@/components/ui/badge'
import { Bot, Sparkles, RefreshCw, ChevronDown, ChevronUp } from 'lucide-react'
import { workflowApi } from '@/lib/api'
import type { WorkflowResult } from '@/types/api'
import ReactMarkdown from 'react-markdown'
import remarkGfm from 'remark-gfm'

interface AIMonitoringAnalysisProps {
    loading?: boolean
    className?: string
}

const COLLAPSED_LENGTH = 300

export function AIMonitoringAnalysis({ loading: parentLoading, className }: AIMonitoringAnalysisProps) {
    const [analysis, setAnalysis] = useState<WorkflowResult | null>(null)
    const [loading, setLoading] = useState(true)
    const [generating, setGenerating] = useState(false)
    const [expanded, setExpanded] = useState(false)
    const pollIntervalRef = useRef<ReturnType<typeof setInterval> | null>(null)

    const fetchAnalysis = async () => {
        try {
            setLoading(true)
            const res = await workflowApi.mySightings()
            setAnalysis(res.data)
        } catch (err) {
            console.error('Failed to fetch monitoring analysis:', err)
        } finally {
            setLoading(false)
        }
    }

    useEffect(() => { fetchAnalysis() }, [])

    useEffect(() => {
        return () => {
            if (pollIntervalRef.current) {
                clearInterval(pollIntervalRef.current)
            }
        }
    }, [])

    const handleGenerate = async () => {
        try {
            setGenerating(true)
            await workflowApi.generateSightings()

            let attempts = 0
            const maxAttempts = 15
            const currentTimestamp = analysis?.timestamp

            pollIntervalRef.current = setInterval(async () => {
                attempts++
                try {
                    const res = await workflowApi.mySightings()
                    if (res.data && (!currentTimestamp || res.data.timestamp !== currentTimestamp)) {
                        setAnalysis(res.data)
                        setGenerating(false)
                        if (pollIntervalRef.current) {
                            clearInterval(pollIntervalRef.current)
                            pollIntervalRef.current = null
                        }
                    }
                } catch (err) {
                    console.error('Polling error:', err)
                }

                if (attempts >= maxAttempts) {
                    setGenerating(false)
                    if (pollIntervalRef.current) {
                        clearInterval(pollIntervalRef.current)
                        pollIntervalRef.current = null
                    }
                    fetchAnalysis()
                }
            }, 2000)
        } catch (err) {
            console.error('Failed to generate monitoring analysis:', err)
            setGenerating(false)
        }
    }

    const isLoading = loading || parentLoading
    const analysisContent = analysis?.analysisText || null
    const lastUpdated = analysis?.timestamp ? new Date(analysis.timestamp).toLocaleString() : null

    const needsExpansion = analysisContent && analysisContent.length > COLLAPSED_LENGTH
    const displayContent = needsExpansion && !expanded
        ? analysisContent.slice(0, COLLAPSED_LENGTH) + '...'
        : analysisContent

    return (
        <Card className={className}>
            <CardHeader className="pb-2">
                <div className="flex items-center justify-between">
                    <div className="flex items-center gap-2">
                        <Bot className="h-5 w-5 text-primary" />
                        <CardTitle className="text-lg font-semibold">AI Wildlife Analysis</CardTitle>
                    </div>
                    <Button size="sm" variant="outline" onClick={handleGenerate} disabled={generating} className="gap-2">
                        {generating ? <RefreshCw className="h-4 w-4 animate-spin" /> : <Sparkles className="h-4 w-4" />}
                        {generating ? 'Generating...' : 'Refresh'}
                    </Button>
                </div>
                {lastUpdated && <CardDescription>Last updated: {lastUpdated}</CardDescription>}
            </CardHeader>
            <CardContent>
                {isLoading ? (
                    <div className="space-y-3">
                        <Skeleton className="h-4 w-full" />
                        <Skeleton className="h-4 w-5/6" />
                        <Skeleton className="h-4 w-4/6" />
                        <Skeleton className="h-4 w-full" />
                        <Skeleton className="h-4 w-3/4" />
                    </div>
                ) : analysisContent ? (
                    <div className="space-y-3">
                        <Badge variant="secondary" className="gap-1 mb-2"><Sparkles className="h-3 w-3" /> AI Generated</Badge>
                        <div className="text-sm prose dark:prose-invert prose-sm max-w-none [&>p]:mb-2 [&>ul]:mb-2 leading-relaxed">
                            <ReactMarkdown remarkPlugins={[remarkGfm]}>
                                {displayContent || ''}
                            </ReactMarkdown>
                        </div>
                        {needsExpansion && (
                            <Button
                                variant="ghost"
                                size="sm"
                                onClick={() => setExpanded(!expanded)}
                                className="gap-1 text-muted-foreground hover:text-foreground p-0 h-auto mt-1"
                            >
                                {expanded ? (
                                    <>
                                        <ChevronUp className="h-4 w-4" />
                                        Show less
                                    </>
                                ) : (
                                    <>
                                        <ChevronDown className="h-4 w-4" />
                                        Show more
                                    </>
                                )}
                            </Button>
                        )}
                    </div>
                ) : (
                    <div className="flex flex-col items-center justify-center py-8 text-center">
                        <Bot className="h-12 w-12 text-muted-foreground mb-3" />
                        <p className="text-sm text-muted-foreground">No analysis available</p>
                        <Button size="sm" variant="outline" onClick={handleGenerate} disabled={generating} className="mt-3 gap-2">
                            <Sparkles className="h-4 w-4" /> Generate Analysis
                        </Button>
                    </div>
                )}
            </CardContent>
        </Card>
    )
}

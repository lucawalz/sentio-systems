export function DashboardFooter() {
    return (
        <footer className="bg-card/60 backdrop-blur-sm border-t border-border mt-12">
            <div className="max-w-7xl mx-auto px-6 py-8">
                <div className="flex items-center justify-between">
                    <div className="text-sm text-muted-foreground">© 2025 Sentio. All rights reserved.</div>
                    <div className="flex items-center space-x-6 text-sm text-muted-foreground">
                        <span>Last updated: 2 minutes ago</span>
                        <span className="flex items-center space-x-2">
                <div className="w-2 h-2 bg-primary rounded-full animate-pulse" />
                <span>System status: Online</span>
              </span>
                    </div>
                </div>
            </div>
        </footer>
    )
}
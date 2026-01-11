import { useEffect } from 'react'
import { useLocation } from 'react-router-dom'

export default function ScrollToTop() {
    const { pathname } = useLocation()

    useEffect(() => {
        // Delay scroll to match exit animation duration (300ms)
        // This prevents the visible "scroll to top" flash before page transition
        const timeout = setTimeout(() => {
            window.scrollTo(0, 0)
        }, 300)

        return () => clearTimeout(timeout)
    }, [pathname])

    return null
}

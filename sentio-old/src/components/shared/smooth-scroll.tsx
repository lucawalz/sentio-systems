import type React from "react"

import { useEffect } from "react"

interface SmoothScrollProps {
  children: React.ReactNode
}

export function SmoothScroll({ children }: SmoothScrollProps) {
  useEffect(() => {
    // Smooth-scroll for in-page anchors via native browser API
    const handleClick = (e: Event) => {
      const link = e.currentTarget as HTMLAnchorElement
      const href = link.getAttribute("href")
      if (href?.startsWith("#")) {
        const target = document.querySelector<HTMLElement>(href)
        if (target) {
          e.preventDefault()
          window.scrollTo({
            top: target.offsetTop,
            behavior: "smooth",
          })
        }
      }
    }

    // Attach to every in-page anchor
    const anchors = document.querySelectorAll<HTMLAnchorElement>('a[href^="#"]')
    anchors.forEach((a) => a.addEventListener("click", handleClick))

    // Refresh ScrollTrigger whenever layout might change
    const onResize = () => {
      if (typeof window !== "undefined") {
        // Lazy-load to avoid a hard import when not needed
        import("gsap/ScrollTrigger").then(({ ScrollTrigger }) => ScrollTrigger.refresh())
      }
    }
    window.addEventListener("resize", onResize)

    return () => {
      anchors.forEach((a) => a.removeEventListener("click", handleClick))
      window.removeEventListener("resize", onResize)
    }
  }, [])

  return <>{children}</>
}

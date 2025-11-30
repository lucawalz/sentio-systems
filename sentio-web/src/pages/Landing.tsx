import React, {useEffect} from "react"
import {gsap} from "gsap"
import {ScrollTrigger} from "gsap/ScrollTrigger"
import {SplitText} from "gsap/SplitText"
import {MotionPathPlugin} from "gsap/MotionPathPlugin"
import {Hero} from "../components/landing/hero"
import {OurStory} from "../components/landing/our-story"
import {ProductOverview} from "../components/landing/product-overview.tsx"
import {Features} from "../components/landing/features"
import {ExperienceCTA} from "../components/landing/experience-cta"
import {Navigation} from "../components/layout/navigation"
import {SmoothScroll} from "../components/shared/smooth-scroll"
import {Link} from "react-router-dom";
import Footer from "../components/footer/footer.tsx";


if (typeof window !== "undefined") {
  gsap.registerPlugin(ScrollTrigger, SplitText, MotionPathPlugin)
}

export default function LandingPage() {
  useEffect(() => {
    // Enhanced scroll setup with better performance
    gsap.set("body", { overflow: "visible" })

    // Force ScrollTrigger refresh after component mount
    const refreshTimer = setTimeout(() => {
      ScrollTrigger.refresh()
    }, 100)

    // Global timeline for coordinated animations
    const masterTimeline = gsap.timeline({ paused: true })

    // Preload critical animations
    gsap.fromTo(
        ".preload-fade",
        { opacity: 0, y: 30 },
        { opacity: 1, y: 0, duration: 0.8, ease: "power3.out", stagger: 0.1 },
    )

    // Handle window resize
    const handleResize = () => {
      ScrollTrigger.refresh()
    }

    window.addEventListener("resize", handleResize)

    return () => {
      clearTimeout(refreshTimer)
      window.removeEventListener("resize", handleResize)
      ScrollTrigger.getAll().forEach((trigger) => trigger.kill())
      masterTimeline.kill()
    }
  }, [])

  return (
      <SmoothScroll>
        <div className="bg-black text-white overflow-x-hidden relative">
          {/* Ambient background effects */}
          <div className="fixed inset-0 pointer-events-none">
            <div
                className="absolute inset-0 bg-[radial-gradient(circle_at_20%_80%,rgba(176,214,255,0.03),transparent_70%)]"/>
            <div
                className="absolute inset-0 bg-[radial-gradient(circle_at_80%_20%,rgba(168,213,186,0.03),transparent_70%)]"/>
            <div
                className="absolute inset-0 bg-[radial-gradient(circle_at_50%_50%,rgba(255,216,168,0.02),transparent_70%)]"/>
          </div>


          <Navigation/>
          <Hero/>
          <OurStory/>
          <ProductOverview/>
          <Features/>
          <ExperienceCTA/>

          <div className="p-10 text-center">
            <Link to="/login">
              <button className="bg-blue-500 hover:bg-blue-600 text-white px-6 py-2 rounded-lg transition">
                Login
              </button>
            </Link>
          </div>
          <Footer/>
          </div>
      </SmoothScroll>

)
}

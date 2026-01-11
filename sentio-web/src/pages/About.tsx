import AboutHeroSection from "@/components/features/about/AboutHeroSection"
import AboutValuesSection from "@/components/features/about/AboutValuesSection"
import AboutMilestonesSection from "@/components/features/about/AboutMilestonesSection"
import TeamSection from "@/components/features/about/TeamSection"
import TestimonialsHighlight from "@/components/features/about/TestimonialsHighlight"
import TestimonialsSection from "@/components/features/about/TestimonialsSection"

export default function About() {
    return (
        <>
            {/* Hero Section with Stats */}
            <AboutHeroSection />

            {/* Values/Principles Section */}
            <AboutValuesSection />

            {/* Milestones/Journey Section */}
            <AboutMilestonesSection />

            {/* Team Section */}
            <TeamSection />

            {/* Testimonials Highlight */}
            <TestimonialsHighlight />

            {/* Testimonials Wall */}
            <TestimonialsSection />
        </>
    )
}

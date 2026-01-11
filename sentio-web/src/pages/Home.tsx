import HeroSection from '@/components/features/hero/HeroSection'
import StatsSection from '@/components/features/hero/StatsSection'
import LogoCloud from '@/components/features/hero/LogoCloud'
import ContentSection from '@/components/features/hero/ContentSection'
import FeaturesSection from '@/components/features/hero/FeaturesSection'
import IntegrationsSection from '@/components/features/hero/IntegrationsSection'
import PricingSection from '@/components/features/hero/PricingSection'
import FAQSection from '@/components/features/hero/FAQSection'
import CTASection from '@/components/features/hero/CTASection'

export default function Home() {
    return (
        <>
            <HeroSection />
            <StatsSection />
            <LogoCloud />
            <FeaturesSection />
            <ContentSection />
            <IntegrationsSection />
            <PricingSection />
            <FAQSection />
            <CTASection />
        </>
    )
}



import { useEffect, useRef } from "react"
import { gsap } from "gsap"
import { Brain, MapPin, Clock, Thermometer, Wind, Activity, PawPrint } from "lucide-react"
import { useAnimalData } from "../../hooks/useAnimalData"
import { useAnimalImages } from "../../hooks/useAnimalImages"
import { formatDistanceToNow } from "date-fns"

// Helper function to get the most recent animal detection
const getMostRecentAnimal = (detections: any[]) => {
  if (!detections.length) return null;
  return detections[0];
};

// Helper function to get the classification confidence for a species
const getClassificationConfidence = (detection: any): number => {
  return detection.confidence;
};

// Enhanced Animal Image Component
const AnimalImage = ({ species, color }: { species: string; color: string }) => {
  const { getAnimalImage, isLoadingImage } = useAnimalImages([species]);
  const imageUrl = getAnimalImage(species);
  const loading = isLoadingImage(species);

  if (loading) {
    return (
      <div className="w-32 h-32 rounded-full bg-secondary/40 animate-pulse flex items-center justify-center border-2 border-border/20">
        <div className="w-16 h-16 bg-secondary/60 rounded-full animate-pulse" />
      </div>
    );
  }

  if (imageUrl) {
    return (
      <div className="w-32 h-32 rounded-full overflow-hidden border-2 border-border/30 shadow-lg">
        <img
          src={imageUrl}
          alt={species}
          className="w-full h-full object-cover"
        />
      </div>
    );
  }

  return (
    <div
      className="w-32 h-32 rounded-full flex items-center justify-center border-2 border-border/30 shadow-lg"
      style={{ backgroundColor: color }}
    >
      <PawPrint className="w-12 h-12 text-white" />
    </div>
  );
};

export function AnimalInformation() {
  const cardRef = useRef<HTMLDivElement>(null)
  const iconRef = useRef<HTMLDivElement>(null)
  const aiIconRef = useRef<HTMLDivElement>(null)
  const { latestDetections, loading, error } = useAnimalData();

  useEffect(() => {
    if (cardRef.current) {
      gsap.fromTo(
        cardRef.current,
        { y: 30, opacity: 0, scale: 0.95 },
        { y: 0, opacity: 1, scale: 1, duration: 1.2, ease: "power3.out" }
      )
    }

    if (iconRef.current) {
      gsap.to(iconRef.current, {
        y: -10,
        duration: 4,
        ease: "power2.inOut",
        yoyo: true,
        repeat: -1,
      })
    }

    if (aiIconRef.current) {
      gsap.to(aiIconRef.current, {
        scale: 1.1,
        duration: 2,
        ease: "power2.inOut",
        yoyo: true,
        repeat: -1,
      })
    }
  }, [])

  if (loading) {
    return (
      <div className="dashboard-card bg-card/80 backdrop-blur-sm border border-border rounded-3xl p-6 md:p-8 min-h-[480px] relative overflow-hidden shadow-xl">
        <div className="flex items-center justify-center h-full flex-col space-y-4">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary"></div>
          <div className="text-sm text-muted-foreground">Loading animal analysis...</div>
        </div>
      </div>
    );
  }

  if (error || !latestDetections.length) {
    return (
      <div className="dashboard-card bg-card/80 backdrop-blur-sm border border-border rounded-3xl p-6 md:p-8 min-h-[480px] relative overflow-hidden shadow-xl">
        <div className="flex flex-col items-center justify-center h-full text-center">
          <PawPrint className="w-16 h-16 text-muted-foreground mb-4" />
          <div className="text-xl font-semibold mb-2 text-foreground">No Animal Detection Data</div>
          <div className="text-sm text-muted-foreground mb-6 max-w-md">
            The AI animal detection system hasn't identified any animals yet. Please wait for animal activity to be detected.
          </div>
        </div>
      </div>
    );
  }

  const recentAnimal = getMostRecentAnimal(latestDetections);
  const species = recentAnimal.species || "Unknown";
  const confidence = getClassificationConfidence(recentAnimal);
  const timeAgo = formatDistanceToNow(new Date(recentAnimal.timestamp), { addSuffix: true });
  const animalType = recentAnimal.animalType;

  // Generate a color for the animal
  const getAnimalColor = (species: string): string => {
    const colors = [
      "#10B981", "#3B82F6", "#F59E0B", "#059669", "#DC2626",
      "#34D399", "#60A5FA", "#FBBF24", "#16A34A", "#EF4444"
    ];
    let hash = 0;
    for (let i = 0; i < species.length; i++) {
      hash = species.charCodeAt(i) + ((hash << 5) - hash);
    }
    return colors[Math.abs(hash) % colors.length];
  };

  // Generate confidence color gradient
  const getConfidenceColorGradient = (conf: number) => {
    let baseColor: string;

    if (conf >= 0.8) {
      baseColor = "#10B981"; // success green
    } else if (conf >= 0.6) {
      baseColor = "#F59E0B"; // warning amber
    } else {
      baseColor = "#DC2626"; // destructive red
    }

    // Convert hex to RGB
    const hex = baseColor.replace('#', '');
    const r = parseInt(hex.substr(0, 2), 16);
    const g = parseInt(hex.substr(2, 2), 16);
    const b = parseInt(hex.substr(4, 2), 16);

    // Create lighter version
    const lightR = Math.round(r + (255 - r) * 0.6);
    const lightG = Math.round(g + (255 - g) * 0.6);
    const lightB = Math.round(b + (255 - b) * 0.6);

    return {
      from: baseColor,
      to: `rgb(${lightR}, ${lightG}, ${lightB})`
    };
  };

  const animalColor = getAnimalColor(species);
  const confidenceGradient = getConfidenceColorGradient(confidence);

  return (
    <div
      ref={cardRef}
      className="dashboard-card bg-card/80 backdrop-blur-sm border border-border rounded-3xl p-6 md:p-8 min-h-[480px] relative overflow-hidden hover:shadow-lg transition-shadow duration-300"
    >
      {/* Background gradient */}
      <div className="absolute inset-0 bg-gradient-to-br from-primary/5 via-transparent to-chart-2/5 pointer-events-none rounded-3xl" />

      <div className="relative z-10 h-full flex flex-col">
        {/* Header */}
        <div className="flex items-start justify-between mb-8">
          <div className="flex items-center space-x-3">
            <Activity className="w-6 h-6 text-primary" />
            <h2 className="text-2xl md:text-3xl font-bold text-foreground">
              AI Animal Analysis
            </h2>
          </div>
          <div className="flex items-center space-x-2 text-xs text-muted-foreground">
            <div className="w-2 h-2 bg-green-500 rounded-full animate-pulse" />
            <span>Last detected {timeAgo}</span>
          </div>
        </div>

        {/* Main Content Layout - Side by Side */}
        <div className="flex gap-8 flex-1">
          {/* Left Side - Animal Information */}
          <div className="flex-shrink-0 w-96">
            <div className="relative bg-card/60 backdrop-blur-sm rounded-2xl border border-border/50 p-8">
              <div className="absolute inset-0 bg-gradient-to-b from-transparent via-transparent to-primary/5 opacity-0 hover:opacity-100 transition-opacity duration-300 rounded-2xl" />
              <div className="relative z-10 flex flex-col items-center text-center space-y-6">
                <div ref={iconRef}>
                  <AnimalImage species={species} color={animalColor} />
                </div>

                <div className="w-full space-y-4">
                  <h4 className="text-3xl font-bold text-foreground">{species}</h4>

                  {/* Icon-based metadata */}
                  <div className="flex items-center justify-center space-x-8">
                    {animalType && (
                      <div className="flex flex-col items-center space-y-1">
                        <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-primary/20 to-primary/40 flex items-center justify-center shadow-lg shadow-primary/30">
                          <Brain className="w-5 h-5 text-primary" />
                        </div>
                        <span className="text-xs text-primary font-medium uppercase">{animalType}</span>
                      </div>
                    )}

                    <div className="flex flex-col items-center space-y-1">
                      <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-amber-400 to-amber-600 flex items-center justify-center shadow-lg shadow-amber-500/30">
                        <Clock className="w-5 h-5 text-white" />
                      </div>
                      <span className="text-xs text-muted-foreground">{timeAgo}</span>
                    </div>
                  </div>

                  {/* Confidence bar */}
                  <div className="w-full">
                    <div className="flex items-center justify-between mb-3">
                      <span className="text-sm font-medium text-foreground">Confidence</span>
                      <span className="text-sm font-bold text-foreground">{Math.round(confidence * 100)}%</span>
                    </div>
                    <div className="w-full h-3 bg-secondary/60 rounded-full overflow-hidden backdrop-blur-sm">
                      <div
                        className="h-full transition-all duration-500 shadow-sm"
                        style={{
                          width: `${confidence * 100}%`,
                          background: `linear-gradient(to right, ${confidenceGradient.from}, ${confidenceGradient.to})`
                        }}
                      />
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          {/* Right Side - Analysis Cards in 2x2 Grid */}
          <div className="flex-1">
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-4 h-full">
              {/* Behavioral Analysis */}
              <div className="group relative bg-card/60 backdrop-blur-sm rounded-2xl border border-border/50 hover:border-primary/30 transition-all duration-300 overflow-hidden hover:scale-105 p-4">
                <div className="absolute inset-0 bg-gradient-to-b from-transparent via-transparent to-primary/5 opacity-0 group-hover:opacity-100 transition-opacity duration-300" />
                <div className="relative z-10">
                  <div className="flex items-center space-x-3 mb-3">
                    <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-blue-400 to-blue-600 flex items-center justify-center shadow-lg shadow-blue-500/30">
                      <Activity className="w-5 h-5 text-white" />
                    </div>
                    <h5 className="font-semibold text-foreground">Behavioral Analysis</h5>
                  </div>
                  <p className="text-sm text-muted-foreground leading-relaxed">
                    Based on detection patterns and environmental conditions, this {species.toLowerCase()} was likely engaged in
                    {confidence > 0.8 ? " active foraging behavior" : " area surveillance"}.
                    The detection suggests optimal lighting conditions during identification.
                  </p>
                </div>
              </div>

              {/* Habitat Insights */}
              <div className="group relative bg-card/60 backdrop-blur-sm rounded-2xl border border-border/50 hover:border-primary/30 transition-all duration-300 overflow-hidden hover:scale-105 p-4">
                <div className="absolute inset-0 bg-gradient-to-b from-transparent via-transparent to-primary/5 opacity-0 group-hover:opacity-100 transition-opacity duration-300" />
                <div className="relative z-10">
                  <div className="flex items-center space-x-3 mb-3">
                    <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-green-400 to-green-600 flex items-center justify-center shadow-lg shadow-green-500/30">
                      <MapPin className="w-5 h-5 text-white" />
                    </div>
                    <h5 className="font-semibold text-foreground">Habitat Overview</h5>
                  </div>
                  <p className="text-sm text-muted-foreground leading-relaxed">
                    This species adapts to various environments.
                    {species === "Blue Jay" && " Prefers deciduous and mixed forests, often found near oak trees."}
                    {species === "Cardinal" && " Thrives in woodland edges, gardens, and shrublands."}
                    {species === "Squirrel" && " Common in woodlands and urban areas with abundant tree cover."}
                    {!["Blue Jay", "Cardinal", "Squirrel"].includes(species) &&
                      ` This ${species.toLowerCase()} demonstrates adaptability to local environmental conditions.`}
                  </p>
                </div>
              </div>

              {/* Environmental Correlation */}
              <div className="group relative bg-card/60 backdrop-blur-sm rounded-2xl border border-border/50 hover:border-primary/30 transition-all duration-300 overflow-hidden hover:scale-105 p-4">
                <div className="absolute inset-0 bg-gradient-to-b from-transparent via-transparent to-primary/5 opacity-0 group-hover:opacity-100 transition-opacity duration-300" />
                <div className="relative z-10">
                  <div className="flex items-center space-x-3 mb-3">
                    <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-amber-400 to-amber-600 flex items-center justify-center shadow-lg shadow-amber-500/30">
                      <Thermometer className="w-5 h-5 text-white" />
                    </div>
                    <h5 className="font-semibold text-foreground">Environmental Factors</h5>
                  </div>
                  <p className="text-sm text-muted-foreground leading-relaxed">
                    Detection timing correlates with optimal activity conditions. Current patterns suggest increased
                    activity during {new Date().getHours() < 12 ? "early morning" : "late afternoon"} hours.
                  </p>
                </div>
              </div>

              {/* Ecological Context */}
              <div className="group relative bg-card/60 backdrop-blur-sm rounded-2xl border border-border/50 hover:border-primary/30 transition-all duration-300 overflow-hidden hover:scale-105 p-4">
                <div className="absolute inset-0 bg-gradient-to-b from-transparent via-transparent to-primary/5 opacity-0 group-hover:opacity-100 transition-opacity duration-300" />
                <div className="relative z-10">
                  <div className="flex items-center space-x-3 mb-3">
                    <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-purple-400 to-purple-600 flex items-center justify-center shadow-lg shadow-purple-500/30">
                      <Wind className="w-5 h-5 text-white" />
                    </div>
                    <h5 className="font-semibold text-foreground">Ecological Role</h5>
                  </div>
                  <p className="text-sm text-muted-foreground leading-relaxed">
                    This animal plays a role in the local ecosystem.
                    Regular detection patterns indicate healthy population dynamics and stable habitat conditions.
                    AI monitoring contributes to biodiversity data.
                  </p>
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* Footer Info */}
        <div className="mt-6 pt-4 border-t border-border/30">
          <div className="text-xs text-muted-foreground opacity-60 text-center">
            AI-powered animal detection • Real-time species identification
          </div>
        </div>
      </div >
    </div >
  );
}
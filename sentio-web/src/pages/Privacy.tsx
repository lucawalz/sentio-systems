export default function Privacy() {
    return (
        <div className="relative min-h-screen px-4 pt-24 pb-16 sm:px-6 lg:px-8">
            {/* Background */}
            <div className="fixed inset-0 -z-10 bg-background" />

            {/* Content */}
            <div className="mx-auto w-full max-w-3xl">
                <div className="rounded-xl border border-gray-200 bg-background shadow-lg">
                    {/* Header */}
                    <div className="border-b border-gray-200 p-6 sm:p-8 md:p-10">
                        <h2 className="mt-4 text-center text-3xl font-bold text-black sm:text-4xl">
                            Privacy Policy
                        </h2>
                        <p className="mt-3 text-center text-sm text-gray-600">
                            Last updated: 03.01.2026
                        </p>
                    </div>

                    {/* Body */}
                    <div className="space-y-8 p-6 text-black leading-relaxed sm:p-8 md:p-10">
                        <section className="space-y-3">
                            <h3 className="text-xl font-semibold text-black">1. Introduction</h3>
                            <p>
                                This Privacy Policy explains how personal data is processed when using this
                                application. This is an open-source portfolio project and is not intended
                                for commercial use.
                            </p>
                            <p>
                                Protecting your personal data is important to us. We process personal data in
                                accordance with the General Data Protection Regulation (GDPR).
                            </p>
                        </section>

                        <section className="space-y-3">
                            <h3 className="text-xl font-semibold text-black">2. Controller</h3>
                            <p>The controller responsible for data processing is:</p>
                            <ul className="list-disc space-y-1 pl-5">
                                <li>Project Name: Sentio Systems</li>
                                <li>Contact: <a href="mailto:security@sentio-systems.com">security@sentio-systems.com</a></li>
                            </ul>
                        </section>

                        <section className="space-y-3">
                            <h3 className="text-xl font-semibold text-black">3. Scope of Data Processing</h3>
                            <p>
                                We process personal data only to the extent necessary to provide the functionality
                                of the application, in particular for user accounts, authentication, and core
                                features of the system.
                            </p>
                            <p>The application consists of:</p>
                            <ul className="list-disc space-y-1 pl-5">
                                <li>a frontend (running on port 5173)</li>
                                <li>a backend API (running on port 8083)</li>
                                <li>a database connected to the backend</li>
                            </ul>
                            <p>
                                Frontend and backend run in a Docker-based environment.
                            </p>
                        </section>

                        <section className="space-y-6">
                            <h3 className="text-xl font-semibold text-black">4. Personal Data We Process</h3>

                            <div className="space-y-2">
                                <h4 className="font-semibold text-black">4.1 User Account Data</h4>
                                <p>When a user registers or logs in, the following data is stored:</p>
                                <ul className="list-disc space-y-1 pl-5">
                                    <li>Name</li>
                                    <li>Username</li>
                                    <li>Email address</li>
                                    <li>Password (stored in encrypted/hashed form)</li>
                                </ul>
                                <p>This data is required to create and manage user accounts.</p>
                            </div>

                            <div className="space-y-2">
                                <h4 className="font-semibold text-black">4.2 Application Data</h4>
                                <p>Depending on how the application is used, the following data may also be stored:</p>
                                <ul className="list-disc space-y-1 pl-5">
                                    <li>Account-related data</li>
                                    <li>Sightings entered by users</li>
                                    <li>Sensor data (when new sensors are added)</li>
                                    <li>Weather-related data associated with sightings or sensors</li>
                                </ul>
                                <p>This data is provided by users or generated through the use of the application.</p>
                            </div>

                            <div className="space-y-2">
                                <h4 className="font-semibold text-black">4.3 Technical Data</h4>
                                <p>When the backend API is accessed, technical data may be processed automatically:</p>
                                <ul className="list-disc space-y-1 pl-5">
                                    <li>IP address (e.g. in server logs)</li>
                                    <li>Date and time of requests</li>
                                </ul>
                                <p>This data is used solely for technical operation, security, and error analysis.</p>
                            </div>
                        </section>

                        <section className="space-y-4">
                            <h3 className="text-xl font-semibold text-black">5. Authentication and Cookies</h3>

                            <div className="space-y-2">
                                <h4 className="font-semibold text-black">5.1 Use of Cookies</h4>
                                <p>
                                    The application uses technically necessary cookies for authentication. The following cookies are used:
                                </p>
                                <ul className="list-disc space-y-2 pl-5">
                                    <li>
                                        <span className="font-semibold">access_token</span>: Used to authenticate API requests. Stored for a short duration.
                                    </li>
                                    <li>
                                        <span className="font-semibold">refresh_token</span>: Used to renew the authentication session. Stored for a limited period (e.g. several days).
                                    </li>
                                </ul>
                            </div>

                            <div className="space-y-2">
                                <h4 className="font-semibold text-black">5.2 Cookie Security</h4>
                                <p>The authentication cookies are configured with the following security measures:</p>
                                <ul className="list-disc space-y-1 pl-5">
                                    <li>httpOnly: Cookies cannot be accessed via JavaScript</li>
                                    <li>SameSite: Protects against CSRF attacks</li>
                                    <li>Secure: Enabled when HTTPS is used</li>
                                </ul>
                                <p>These cookies are required for the secure operation of the application and cannot be disabled.</p>
                            </div>
                        </section>

                        <section className="space-y-3">
                            <h3 className="text-xl font-semibold text-black">6. Legal Basis for Processing</h3>
                            <p>Personal data is processed on the following legal bases:</p>
                            <ul className="list-disc space-y-1 pl-5">
                                <li>Art. 6(1)(b) GDPR – performance of a contract (user account and application usage)</li>
                                <li>Art. 6(1)(f) GDPR – legitimate interest in ensuring security and technical functionality</li>
                            </ul>
                        </section>

                        <section className="space-y-3">
                            <h3 className="text-xl font-semibold text-black">7. Data Storage and Retention</h3>
                            <ul className="list-disc space-y-1 pl-5">
                                <li>User account data is stored as long as the account exists.</li>
                                <li>Authentication cookies expire automatically.</li>
                                <li>Application data is stored as long as required for the project functionality.</li>
                                <li>Log data is stored only temporarily for technical and security purposes.</li>
                            </ul>
                        </section>

                        <section className="space-y-3">
                            <h3 className="text-xl font-semibold text-black">8. Data Sharing</h3>
                            <p>
                                Personal data is not shared with third parties. The application is currently not hosted
                                by external providers and runs in a local Docker environment.
                            </p>
                        </section>

                        <section className="space-y-3">
                            <h3 className="text-xl font-semibold text-black">9. Analytics and Tracking</h3>
                            <p>No tracking, analytics, or marketing tools are used. In particular:</p>
                            <ul className="list-disc space-y-1 pl-5">
                                <li>No third-party tracking cookies</li>
                                <li>No analytics services (e.g. Google Analytics)</li>
                                <li>No profiling or automated decision-making</li>
                            </ul>
                        </section>

                        <section className="space-y-3">
                            <h3 className="text-xl font-semibold text-black">10. Your Rights</h3>
                            <p>Under the GDPR, users have the following rights:</p>
                            <ul className="list-disc space-y-1 pl-5">
                                <li>Right to access their personal data</li>
                                <li>Right to rectification</li>
                                <li>Right to erasure</li>
                                <li>Right to restriction of processing</li>
                                <li>Right to data portability</li>
                                <li>Right to object to processing</li>
                            </ul>
                            <p>Users also have the right to lodge a complaint with a data protection authority.</p>
                        </section>

                        <section className="space-y-3">
                            <h3 className="text-xl font-semibold text-black">11. Changes to This Privacy Policy</h3>
                            <p>
                                This Privacy Policy may be updated if the functionality of the application changes
                                or if new features are added.
                            </p>
                        </section>
                    </div>
                </div>
            </div>
        </div>
    );
}
import React from 'react';

interface LayoutProps {
    children: React.ReactNode;
}

export function Layout({ children }: LayoutProps) {
    return (
        <div className="font-geist-sans antialiased">
            {children}
        </div>
    );
}
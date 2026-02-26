import { describe, it, expect } from 'vitest';
import { render, screen } from '@/test/test-utils';
import { Badge } from './badge';

describe('Badge', () => {
  it('renders with text content', () => {
    render(<Badge>New</Badge>);

    expect(screen.getByText('New')).toBeInTheDocument();
  });

  it('renders with default variant', () => {
    const { container } = render(<Badge>Default</Badge>);
    const badge = container.firstChild;

    expect(badge).toBeInTheDocument();
  });

  it('renders with different variants', () => {
    const { rerender } = render(<Badge variant="secondary">Secondary</Badge>);

    expect(screen.getByText('Secondary')).toBeInTheDocument();

    rerender(<Badge variant="destructive">Destructive</Badge>);
    expect(screen.getByText('Destructive')).toBeInTheDocument();

    rerender(<Badge variant="outline">Outline</Badge>);
    expect(screen.getByText('Outline')).toBeInTheDocument();
  });

  it('applies custom className', () => {
    const { container } = render(<Badge className="custom-badge">Custom</Badge>);
    const badge = container.firstChild;

    expect(badge).toHaveClass('custom-badge');
  });

  it('renders as different HTML element when asChild is true', () => {
    render(
      <Badge asChild>
        <a href="/test">Link Badge</a>
      </Badge>
    );

    const link = screen.getByRole('link', { name: /link badge/i });
    expect(link).toBeInTheDocument();
    expect(link).toHaveAttribute('href', '/test');
  });
});

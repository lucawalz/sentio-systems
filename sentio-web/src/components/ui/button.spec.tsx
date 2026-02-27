import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@/test/test-utils';
import { Button } from './button';
import userEvent from '@testing-library/user-event';

describe('Button', () => {
  it('renders with default variant and size', () => {
    render(<Button>Click me</Button>);

    const button = screen.getByRole('button', { name: /click me/i });
    expect(button).toBeInTheDocument();
    expect(button).toHaveAttribute('data-variant', 'default');
    expect(button).toHaveAttribute('data-size', 'default');
  });

  it('renders with different variants', () => {
    const { rerender } = render(<Button variant="destructive">Delete</Button>);

    let button = screen.getByRole('button', { name: /delete/i });
    expect(button).toHaveAttribute('data-variant', 'destructive');

    rerender(<Button variant="outline">Outline</Button>);
    button = screen.getByRole('button', { name: /outline/i });
    expect(button).toHaveAttribute('data-variant', 'outline');

    rerender(<Button variant="ghost">Ghost</Button>);
    button = screen.getByRole('button', { name: /ghost/i });
    expect(button).toHaveAttribute('data-variant', 'ghost');
  });

  it('renders with different sizes', () => {
    const { rerender } = render(<Button size="sm">Small</Button>);

    let button = screen.getByRole('button', { name: /small/i });
    expect(button).toHaveAttribute('data-size', 'sm');

    rerender(<Button size="lg">Large</Button>);
    button = screen.getByRole('button', { name: /large/i });
    expect(button).toHaveAttribute('data-size', 'lg');

    rerender(<Button size="icon">Icon</Button>);
    button = screen.getByRole('button', { name: /icon/i });
    expect(button).toHaveAttribute('data-size', 'icon');
  });

  it('handles click events', async () => {
    const user = userEvent.setup();
    const handleClick = vi.fn();

    render(<Button onClick={handleClick}>Click me</Button>);

    const button = screen.getByRole('button', { name: /click me/i });
    await user.click(button);

    expect(handleClick).toHaveBeenCalledTimes(1);
  });

  it('can be disabled', async () => {
    const user = userEvent.setup();
    const handleClick = vi.fn();

    render(<Button disabled onClick={handleClick}>Disabled</Button>);

    const button = screen.getByRole('button', { name: /disabled/i });
    expect(button).toBeDisabled();

    await user.click(button);
    expect(handleClick).not.toHaveBeenCalled();
  });

  it('renders as child component when asChild is true', () => {
    render(
      <Button asChild>
        <a href="/test">Link Button</a>
      </Button>
    );

    const link = screen.getByRole('link', { name: /link button/i });
    expect(link).toBeInTheDocument();
    expect(link).toHaveAttribute('href', '/test');
  });

  it('applies custom className', () => {
    render(<Button className="custom-class">Custom</Button>);

    const button = screen.getByRole('button', { name: /custom/i });
    expect(button).toHaveClass('custom-class');
  });

  it('renders with icon', () => {
    render(
      <Button>
        <svg data-testid="test-icon" />
        With Icon
      </Button>
    );

    expect(screen.getByTestId('test-icon')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /with icon/i })).toBeInTheDocument();
  });
});

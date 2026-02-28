import { describe, it, expect } from 'vitest';
import { render, screen } from '@/test/test-utils';
import { Label } from './label';

describe('Label', () => {
  it('renders label element', () => {
    render(<Label>Username</Label>);

    expect(screen.getByText('Username')).toBeInTheDocument();
  });

  it('associates with form control via htmlFor', () => {
    render(
      <>
        <Label htmlFor="test-input">Test Label</Label>
        <input id="test-input" />
      </>
    );

    const label = screen.getByText('Test Label');
    expect(label).toHaveAttribute('for', 'test-input');
  });

  it('applies custom className', () => {
    render(<Label className="custom-label">Custom</Label>);

    const label = screen.getByText('Custom');
    expect(label).toHaveClass('custom-label');
  });

  it('renders with complex children', () => {
    render(
      <Label>
        <span>Required</span> Username
      </Label>
    );

    expect(screen.getByText('Required')).toBeInTheDocument();
    expect(screen.getByText(/username/i)).toBeInTheDocument();
  });
});

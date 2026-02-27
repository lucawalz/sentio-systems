import { describe, it, expect } from 'vitest';
import { render } from '@/test/test-utils';
import { Logo, LogoIcon, LogoStroke } from './Logo';

describe('Logo Components', () => {
  describe('Logo', () => {
    it('renders the full logo SVG', () => {
      const { container } = render(<Logo />);
      const svg = container.querySelector('svg');

      expect(svg).toBeInTheDocument();
      expect(svg).toHaveAttribute('viewBox', '0 0 1657 341');
    });

    it('applies custom className', () => {
      const { container } = render(<Logo className="custom-logo" />);
      const svg = container.querySelector('svg');

      expect(svg).toHaveClass('custom-logo');
    });

    it('has default height class', () => {
      const { container } = render(<Logo />);
      const svg = container.querySelector('svg');

      expect(svg).toHaveClass('h-8');
    });
  });

  describe('LogoIcon', () => {
    it('renders the icon logo SVG', () => {
      const { container } = render(<LogoIcon />);
      const svg = container.querySelector('svg');

      expect(svg).toBeInTheDocument();
      expect(svg).toHaveAttribute('viewBox', '100 90 240 160');
    });

    it('applies custom className', () => {
      const { container } = render(<LogoIcon className="custom-icon" />);
      const svg = container.querySelector('svg');

      expect(svg).toHaveClass('custom-icon');
    });

    it('has default size class', () => {
      const { container } = render(<LogoIcon />);
      const svg = container.querySelector('svg');

      expect(svg).toHaveClass('size-6');
    });
  });

  describe('LogoStroke', () => {
    it('renders the stroke logo SVG', () => {
      const { container } = render(<LogoStroke />);
      const svg = container.querySelector('svg');

      expect(svg).toBeInTheDocument();
      expect(svg).toHaveAttribute('viewBox', '100 90 240 160');
    });

    it('applies custom className', () => {
      const { container } = render(<LogoStroke className="custom-stroke" />);
      const svg = container.querySelector('svg');

      expect(svg).toHaveClass('custom-stroke');
    });

    it('has stroke styling', () => {
      const { container } = render(<LogoStroke />);
      const path = container.querySelector('path');

      expect(path).toHaveAttribute('fill', 'none');
      expect(path).toHaveAttribute('stroke', 'currentColor');
      // SVG attributes use kebab-case in DOM
      expect(path).toHaveAttribute('stroke-width', '4');
    });
  });
});

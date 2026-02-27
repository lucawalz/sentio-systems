import { describe, it, expect } from 'vitest';
import { render, screen } from '@/test/test-utils';
import { Card, CardHeader, CardTitle, CardDescription, CardContent, CardFooter } from './card';

describe('Card Components', () => {
  describe('Card', () => {
    it('renders card element', () => {
      render(<Card>Card content</Card>);

      expect(screen.getByText('Card content')).toBeInTheDocument();
    });

    it('applies custom className', () => {
      const { container } = render(<Card className="custom-card">Content</Card>);

      const card = container.firstChild;
      expect(card).toHaveClass('custom-card');
    });
  });

  describe('CardHeader', () => {
    it('renders header element', () => {
      render(<CardHeader>Header content</CardHeader>);

      expect(screen.getByText('Header content')).toBeInTheDocument();
    });
  });

  describe('CardTitle', () => {
    it('renders title element', () => {
      render(<CardTitle>Title</CardTitle>);

      expect(screen.getByText('Title')).toBeInTheDocument();
    });
  });

  describe('CardDescription', () => {
    it('renders description element', () => {
      render(<CardDescription>Description text</CardDescription>);

      expect(screen.getByText('Description text')).toBeInTheDocument();
    });
  });

  describe('CardContent', () => {
    it('renders content element', () => {
      render(<CardContent>Main content</CardContent>);

      expect(screen.getByText('Main content')).toBeInTheDocument();
    });
  });

  describe('CardFooter', () => {
    it('renders footer element', () => {
      render(<CardFooter>Footer content</CardFooter>);

      expect(screen.getByText('Footer content')).toBeInTheDocument();
    });
  });

  describe('Complete Card', () => {
    it('renders all card components together', () => {
      render(
        <Card>
          <CardHeader>
            <CardTitle>Test Card</CardTitle>
            <CardDescription>This is a test card</CardDescription>
          </CardHeader>
          <CardContent>
            <p>Card main content</p>
          </CardContent>
          <CardFooter>
            <button>Action</button>
          </CardFooter>
        </Card>
      );

      expect(screen.getByText('Test Card')).toBeInTheDocument();
      expect(screen.getByText('This is a test card')).toBeInTheDocument();
      expect(screen.getByText('Card main content')).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /action/i })).toBeInTheDocument();
    });
  });
});

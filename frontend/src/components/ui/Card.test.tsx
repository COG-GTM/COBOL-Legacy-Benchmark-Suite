import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { Card } from './Card';

describe('Card', () => {
  it('renders children', () => {
    render(<Card>Hello</Card>);
    expect(screen.getByText('Hello')).toBeInTheDocument();
  });

  it('renders title when provided', () => {
    render(<Card title="My Title">content</Card>);
    expect(screen.getByText('My Title')).toBeInTheDocument();
  });

  it('renders actions when provided', () => {
    render(
      <Card title="T" actions={<button>Act</button>}>
        content
      </Card>,
    );
    expect(screen.getByRole('button', { name: 'Act' })).toBeInTheDocument();
  });

  it('does not render header row when no title or actions', () => {
    const { container } = render(<Card>bare</Card>);
    expect(container.querySelector('.border-b')).toBeNull();
  });

  it('applies custom className', () => {
    const { container } = render(<Card className="my-cls">x</Card>);
    expect(container.firstChild).toHaveClass('my-cls');
  });
});

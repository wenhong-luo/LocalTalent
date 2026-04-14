import { render, screen } from '@testing-library/react';
import App from './App';

describe('App', () => {
  it('renders scaffold heading and state placeholders', () => {
    render(<App />);

    expect(screen.getByText('P0 Frontend Scaffold')).toBeInTheDocument();
    expect(screen.getByLabelText('state-view-loading')).toBeInTheDocument();
    expect(screen.getByLabelText('state-view-error')).toBeInTheDocument();
    expect(screen.getByLabelText('state-view-forbidden')).toBeInTheDocument();
    expect(screen.getByLabelText('state-view-retrying')).toBeInTheDocument();
  });
});

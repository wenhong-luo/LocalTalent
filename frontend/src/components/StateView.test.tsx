import { fireEvent, render, screen } from '@testing-library/react';
import { StateView, type StateVariant } from './StateView';

describe('StateView', () => {
  it.each<StateVariant>(['loading', 'error', 'forbidden', 'retrying'])(
    'renders %s variant',
    (variant) => {
      render(<StateView variant={variant} />);

      expect(screen.getByLabelText(`state-view-${variant}`)).toBeInTheDocument();
    }
  );

  it('calls retry handler for the error variant', () => {
    const onRetry = vi.fn();

    render(
      <StateView
        variant="error"
        title="请求失败"
        description="请重试"
        retryLabel="重新加载"
        onRetry={onRetry}
      />
    );

    fireEvent.click(screen.getByRole('button', { name: '重新加载' }));

    expect(onRetry).toHaveBeenCalledTimes(1);
  });
});

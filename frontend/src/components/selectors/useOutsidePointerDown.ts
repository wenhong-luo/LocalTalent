'use client';

import { useEffect, type RefObject } from 'react';

export function useOutsidePointerDown<T extends HTMLElement>(
  rootRef: RefObject<T | null>,
  enabled: boolean,
  onOutside: () => void
) {
  useEffect(() => {
    if (!enabled) {
      return;
    }

    function handlePointerDown(event: PointerEvent) {
      const target = event.target;
      if (!(target instanceof Node)) {
        return;
      }
      if (rootRef.current?.contains(target)) {
        return;
      }
      onOutside();
    }

    document.addEventListener('pointerdown', handlePointerDown);
    return () => document.removeEventListener('pointerdown', handlePointerDown);
  }, [enabled, onOutside, rootRef]);
}

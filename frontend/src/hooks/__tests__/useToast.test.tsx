import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { renderHook, act } from '@testing-library/react'
import {
  toast,
  addToast,
  dismissToast,
  clearAllToasts,
  useToast,
} from '@/hooks/useToast'
import { createAppError } from '@/types/errors'

describe('useToast store functions', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    clearAllToasts()
  })

  afterEach(() => {
    clearAllToasts()
    vi.useRealTimers()
  })

  it('toast() adds a toast to the store and returns an id', () => {
    const id = toast('Hello')
    expect(id).toMatch(/^toast-\d+$/)

    const { result } = renderHook(() => useToast())
    expect(result.current.toasts.some((t) => t.id === id)).toBe(true)
  })

  it('addToast() adds an AppError-based toast', () => {
    const error = createAppError('TEST', 'Test error', 'error')
    const id = addToast(error)
    expect(id).toMatch(/^toast-\d+$/)

    const { result } = renderHook(() => useToast())
    const found = result.current.toasts.find((t) => t.id === id)
    expect(found).toBeDefined()
    expect(found!.error.message).toBe('Test error')
    expect(found!.error.severity).toBe('error')
  })

  it('dismissToast() removes a specific toast by id', () => {
    const id1 = toast('First')
    const id2 = toast('Second')

    const { result } = renderHook(() => useToast())
    expect(result.current.toasts).toHaveLength(2)

    act(() => {
      dismissToast(id1)
    })

    expect(result.current.toasts).toHaveLength(1)
    expect(result.current.toasts[0].id).toBe(id2)
  })

  it('clearAllToasts() removes all toasts', () => {
    toast('A')
    toast('B')
    toast('C')

    const { result } = renderHook(() => useToast())
    expect(result.current.toasts).toHaveLength(3)

    act(() => {
      clearAllToasts()
    })

    expect(result.current.toasts).toHaveLength(0)
  })

  it('auto-dismisses error toasts after 8000ms', () => {
    const id = toast('Error toast', 'error')

    const { result } = renderHook(() => useToast())
    expect(result.current.toasts.some((t) => t.id === id)).toBe(true)

    act(() => {
      vi.advanceTimersByTime(7999)
    })
    expect(result.current.toasts.some((t) => t.id === id)).toBe(true)

    act(() => {
      vi.advanceTimersByTime(1)
    })
    expect(result.current.toasts.some((t) => t.id === id)).toBe(false)
  })

  it('auto-dismisses warning toasts after 5000ms', () => {
    const id = toast('Warning toast', 'warning')

    const { result } = renderHook(() => useToast())
    expect(result.current.toasts.some((t) => t.id === id)).toBe(true)

    act(() => {
      vi.advanceTimersByTime(4999)
    })
    expect(result.current.toasts.some((t) => t.id === id)).toBe(true)

    act(() => {
      vi.advanceTimersByTime(1)
    })
    expect(result.current.toasts.some((t) => t.id === id)).toBe(false)
  })

  it('auto-dismisses info toasts after 3000ms', () => {
    const id = toast('Info toast', 'info')

    const { result } = renderHook(() => useToast())
    expect(result.current.toasts.some((t) => t.id === id)).toBe(true)

    act(() => {
      vi.advanceTimersByTime(2999)
    })
    expect(result.current.toasts.some((t) => t.id === id)).toBe(true)

    act(() => {
      vi.advanceTimersByTime(1)
    })
    expect(result.current.toasts.some((t) => t.id === id)).toBe(false)
  })

  it('respects custom duration option', () => {
    const id = toast('Custom duration', 'info', { duration: 10000 })

    const { result } = renderHook(() => useToast())
    expect(result.current.toasts.some((t) => t.id === id)).toBe(true)

    act(() => {
      vi.advanceTimersByTime(9999)
    })
    expect(result.current.toasts.some((t) => t.id === id)).toBe(true)

    act(() => {
      vi.advanceTimersByTime(1)
    })
    expect(result.current.toasts.some((t) => t.id === id)).toBe(false)
  })

  it('limits to MAX_TOASTS (5) toasts, removing oldest first', () => {
    const ids: string[] = []
    for (let i = 0; i < 6; i++) {
      ids.push(toast(`Toast ${i}`, 'info', { duration: 0 }))
    }

    const { result } = renderHook(() => useToast())
    expect(result.current.toasts).toHaveLength(5)
    expect(result.current.toasts[0].id).toBe(ids[1])
    expect(result.current.toasts[4].id).toBe(ids[5])
    expect(result.current.toasts.some((t) => t.id === ids[0])).toBe(false)
  })

  it('useToast hook returns current toasts and action functions', () => {
    const { result } = renderHook(() => useToast())

    expect(result.current.toasts).toBeDefined()
    expect(Array.isArray(result.current.toasts)).toBe(true)
    expect(typeof result.current.addToast).toBe('function')
    expect(typeof result.current.toast).toBe('function')
    expect(typeof result.current.dismiss).toBe('function')
    expect(typeof result.current.clearAll).toBe('function')

    let id: string
    act(() => {
      id = result.current.toast('Hook toast', 'warning')
    })
    expect(result.current.toasts.some((t) => t.id === id!)).toBe(true)

    act(() => {
      result.current.dismiss(id!)
    })
    expect(result.current.toasts.some((t) => t.id === id!)).toBe(false)
  })
})

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import {
  handleApiError,
  configureApiErrorInterceptor,
  fetchWithErrorHandling,
} from '@/services/apiErrorInterceptor'
import { clearAllToasts } from '@/hooks/useToast'
import * as toastModule from '@/hooks/useToast'

describe('apiErrorInterceptor', () => {
  beforeEach(() => {
    clearAllToasts()
    configureApiErrorInterceptor({
      onUnauthorized: undefined,
      onNotFound: undefined,
      onError: undefined,
    })
  })

  afterEach(() => {
    clearAllToasts()
    vi.restoreAllMocks()
  })

  describe('handleApiError', () => {
    it('creates an AppError with correct HTTP status code', () => {
      const error = handleApiError(500)
      expect(error.code).toBe('HTTP_500')
      expect(error.severity).toBeDefined()
      expect(error.message).toBeDefined()
    })

    it('maps 4xx to warning severity, 5xx to error severity', () => {
      const warning = handleApiError(400)
      expect(warning.severity).toBe('warning')

      const error = handleApiError(500)
      expect(error.severity).toBe('error')

      const notFound = handleApiError(404)
      expect(notFound.severity).toBe('warning')

      const serverError = handleApiError(503)
      expect(serverError.severity).toBe('error')
    })

    it('calls onUnauthorized callback for 401 status', () => {
      const onUnauthorized = vi.fn()
      configureApiErrorInterceptor({ onUnauthorized })

      handleApiError(401)
      expect(onUnauthorized).toHaveBeenCalledOnce()
    })

    it('calls onNotFound callback for 404 status', () => {
      const onNotFound = vi.fn()
      configureApiErrorInterceptor({ onNotFound })

      handleApiError(404)
      expect(onNotFound).toHaveBeenCalledOnce()
    })

    it('adds toast for other 4xx/5xx errors', () => {
      const addToastSpy = vi.spyOn(toastModule, 'addToast')

      handleApiError(400)
      expect(addToastSpy).toHaveBeenCalledOnce()

      addToastSpy.mockClear()

      handleApiError(500)
      expect(addToastSpy).toHaveBeenCalledOnce()
    })

    it('extracts message from JSON body (message, error, detail fields)', () => {
      const fromMessage = handleApiError(400, { message: 'Bad input data' })
      expect(fromMessage.message).toBe('Bad input data')

      const fromError = handleApiError(400, { error: 'Validation failed' })
      expect(fromError.message).toBe('Validation failed')

      const fromDetail = handleApiError(400, { detail: 'Missing field' })
      expect(fromDetail.message).toBe('Missing field')
    })

    it('uses default messages when body is empty', () => {
      const error = handleApiError(400)
      expect(error.message).toBe(
        'Invalid request. Please check your input and try again.',
      )

      const serverError = handleApiError(500)
      expect(serverError.message).toBe(
        'An internal server error occurred. Please try again later.',
      )
    })

    it('configureApiErrorInterceptor updates the config', () => {
      const onUnauthorized = vi.fn()
      const onNotFound = vi.fn()
      configureApiErrorInterceptor({ onUnauthorized, onNotFound })

      handleApiError(401)
      expect(onUnauthorized).toHaveBeenCalledOnce()

      handleApiError(404)
      expect(onNotFound).toHaveBeenCalledOnce()
    })

    it('default messages are correct for common HTTP status codes', () => {
      const expectations: Record<number, string> = {
        400: 'Invalid request. Please check your input and try again.',
        401: 'Your session has expired. Please log in again.',
        403: 'You do not have permission to perform this action.',
        404: 'The requested resource was not found.',
        500: 'An internal server error occurred. Please try again later.',
        502: 'The server is temporarily unavailable. Please try again later.',
        503: 'The service is currently unavailable. Please try again later.',
      }

      for (const [status, message] of Object.entries(expectations)) {
        const error = handleApiError(Number(status))
        expect(error.message).toBe(message)
      }
    })
  })

  describe('fetchWithErrorHandling', () => {
    beforeEach(() => {
      vi.stubGlobal('fetch', vi.fn())
    })

    afterEach(() => {
      vi.unstubAllGlobals()
    })

    it('returns response for ok responses', async () => {
      const mockResponse = {
        ok: true,
        status: 200,
        json: vi.fn().mockResolvedValue({ data: 'test' }),
      } as unknown as Response

      vi.mocked(fetch).mockResolvedValue(mockResponse)

      const result = await fetchWithErrorHandling('/api/test')
      expect(result).toBe(mockResponse)
    })

    it('calls handleApiError for non-ok responses', async () => {
      const mockResponse = {
        ok: false,
        status: 500,
        headers: new Headers({ 'content-type': 'application/json' }),
        json: vi.fn().mockResolvedValue({ message: 'Server error' }),
        clone() { return { ...this, json: vi.fn().mockResolvedValue({ message: 'Server error' }) } },
      } as unknown as Response

      vi.mocked(fetch).mockResolvedValue(mockResponse)

      const addToastSpy = vi.spyOn(toastModule, 'addToast')
      await fetchWithErrorHandling('/api/test')
      expect(addToastSpy).toHaveBeenCalled()
    })

    it('handles JSON and text response bodies', async () => {
      const jsonResponse = {
        ok: false,
        status: 400,
        headers: new Headers({ 'content-type': 'application/json' }),
        json: vi.fn().mockResolvedValue({ message: 'JSON error' }),
        clone() { return { ...this, json: vi.fn().mockResolvedValue({ message: 'JSON error' }) } },
      } as unknown as Response

      vi.mocked(fetch).mockResolvedValue(jsonResponse)

      const addToastSpy = vi.spyOn(toastModule, 'addToast')
      await fetchWithErrorHandling('/api/json')
      expect(addToastSpy).toHaveBeenCalled()
      const jsonError = addToastSpy.mock.calls[0][0]
      expect(jsonError.message).toBe('JSON error')

      addToastSpy.mockClear()

      const textResponse = {
        ok: false,
        status: 400,
        headers: new Headers({ 'content-type': 'text/plain' }),
        text: vi.fn().mockResolvedValue('Plain text error'),
        clone() { return { ...this, text: vi.fn().mockResolvedValue('Plain text error') } },
      } as unknown as Response

      vi.mocked(fetch).mockResolvedValue(textResponse)

      await fetchWithErrorHandling('/api/text')
      expect(addToastSpy).toHaveBeenCalled()
      const textError = addToastSpy.mock.calls[0][0]
      expect(textError.message).toBe('Plain text error')
    })
  })
})

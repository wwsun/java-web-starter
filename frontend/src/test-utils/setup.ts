// frontend/src/test-utils/setup.ts
import '@testing-library/jest-dom'
import { server } from './server'
import { beforeAll, afterAll, afterEach } from 'vitest'

beforeAll(() => server.listen({ onUnhandledRequest: 'warn' }))
afterEach(() => server.resetHandlers())
afterAll(() => server.close())

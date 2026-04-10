// frontend/e2e/auth.spec.ts
import { test, expect } from '@playwright/test'

/**
 * 认证完整流程 E2E 测试
 * 前提：后端服务运行在 localhost:8080，前端 dev server 运行在 localhost:5173
 */
test.describe('认证流程', () => {
  test('未登录访问首页应重定向到登录页', async ({ page }) => {
    await page.goto('/')
    await expect(page).toHaveURL(/\/login/)
  })

  test('登录成功后跳转到 Dashboard', async ({ page }) => {
    await page.goto('/login')

    await page.fill('#username', 'admin')
    await page.fill('#password', 'admin123')
    await page.click('button[type="submit"]')

    await expect(page).toHaveURL('http://localhost:5173/', { timeout: 10_000 })
    await expect(page.locator('h1')).toContainText('仪表盘')
  })

  test('登出后重定向到登录页', async ({ page }) => {
    // 先登录
    await page.goto('/login')
    await page.fill('#username', 'admin')
    await page.fill('#password', 'admin123')
    await page.click('button[type="submit"]')
    await page.waitForURL('http://localhost:5173/')

    // 点击退出登录
    await page.click('[data-testid="logout-btn"]')
    await expect(page).toHaveURL(/\/login/)
  })
})

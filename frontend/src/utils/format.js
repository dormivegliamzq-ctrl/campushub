/**
 * 时间显示工具：把 ISO 时间转成友好格式
 */
export function formatTime(iso) {
  if (!iso) return ''
  const date = new Date(iso)
  const now = new Date()
  const diff = now - date

  const minute = 60 * 1000
  const hour = 60 * minute
  const day = 24 * hour

  if (diff < minute) return '刚刚'
  if (diff < hour) return `${Math.floor(diff / minute)} 分钟前`
  if (diff < day) return `${Math.floor(diff / hour)} 小时前`
  if (diff < 7 * day) return `${Math.floor(diff / day)} 天前`

  const pad = (n) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

/**
 * 根据用户 ID 生成稳定的头像底色（不用图片时的首字母头像）
 */
export function avatarColor(id) {
  const palette = ['#3f7d5c', '#b0653a', '#4a6fa5', '#8a6d9e', '#a0522d', '#2f7d80']
  return palette[Number(id || 0) % palette.length]
}

/** 昵称或用户名的首字符，用于头像占位 */
export function initialChar(user) {
  const name = user?.nickname || user?.username || '?'
  return name.charAt(0).toUpperCase()
}

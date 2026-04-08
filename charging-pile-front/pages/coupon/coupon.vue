# 排查跳转问题

## 🔍 常见原因分析

跳转失败通常有以下几个原因：

1. **`pages.json` 没有注册目标页面**
2. **路径写错**（大小写/斜杠问题）
3. **`goDetail` 方法中数据为空导致提前 return**
4. **点击事件被子元素阻止冒泡**

---

## ✅ 完整修复版本

### 第一步：确认 `pages.json`

```json
{
  "pages": [
    {
      "path": "pages/index/index",
      "style": {
        "navigationBarTitleText": "首页"
      }
    },
    {
      "path": "pages/coupon/coupon-list",
      "style": {
        "navigationStyle": "custom",
        "navigationBarTitleText": "充电优惠券"
      }
    },
    {
      "path": "pages/coupon/coupon-detail",
      "style": {
        "navigationBarTitleText": "优惠券详情",
        "navigationBarBackgroundColor": "#ffffff",
        "navigationBarTextStyle": "black"
      }
    }
  ]
}
```

> ⚠️ **注意**：`pages.json` 中第一项必须是入口页，`coupon-detail` 必须在列表中存在

---

### 第二步：修复列表页跳转逻辑

**核心问题：** `grab-btn` 上有 `@click.stop`，会阻止冒泡，同时卡片整体也绑了点击，两者冲突时容易失效。统一改为只在卡片层处理跳转。

```vue
<template>
  <view class="page">

    <!-- 自定义顶部导航 -->
    <view class="nav-bar" :style="{ paddingTop: statusBarH + 'px' }">
      <view class="nav-main">
        <view class="nav-left">
          <text class="nav-title">充电优惠券</text>
          <text class="nav-sub">每日精选好券，充电省更多</text>
        </view>
        <view class="nav-right">
          <view class="nav-icon-wrap">
            <text class="nav-icon-txt">🎫</text>
            <view class="nav-badge">
              <text class="nav-badge-text">6</text>
            </view>
          </view>
        </view>
      </view>
      <!-- 搜索框 -->
      <view class="search-box">
        <text class="search-icon-txt">🔍</text>
        <input
          class="search-input"
          v-model="keyword"
          placeholder="搜索优惠券..."
          placeholder-style="color:rgba(255,255,255,0.7);font-size:28rpx;"
          @input="onSearch"
        />
      </view>
    </view>

    <!-- Tab分类 -->
    <view class="tab-wrap">
      <scroll-view scroll-x class="tab-scroll" :show-scrollbar="false">
        <view class="tab-list">
          <view
            v-for="(tab, i) in tabs"
            :key="i"
            class="tab-item"
            :class="{ 'tab-active': activeTab === i }"
            @click="switchTab(i)"
          >
            <text class="tab-txt" :class="{ 'tab-txt-active': activeTab === i }">
              {{ tab.label }}
            </text>
            <view v-if="activeTab === i" class="tab-line"></view>
          </view>
        </view>
      </scroll-view>
    </view>

    <!-- 统计栏 -->
    <view class="stat-bar">
      <text class="stat-txt">
        共 <text class="stat-num">{{ showList.length }}</text> 张优惠券
      </text>
      <text class="stat-link" @click="goDetail(showList[0])">点击查看详情 ›</text>
    </view>

    <!-- 优惠券列表 -->
    <scroll-view
      class="list-scroll"
      scroll-y
      :refresher-enabled="true"
      :refresher-triggered="refreshing"
      @refresherrefresh="onRefresh"
    >
      <view class="list-wrap">
        <view
          v-for="(item, i) in showList"
          :key="item.id"
          class="coupon-card"
          @click="goDetail(item)"
        >
          <!-- 左色块 -->
          <view class="card-left" :style="{ background: item.leftBg }">
            <!-- 顶部锯齿 -->
            <view class="sawtooth-row">
              <view v-for="n in 8" :key="n" class="saw-item"></view>
            </view>
            <!-- 左侧文字 -->
            <view class="left-content">
              <text class="left-main">{{ item.leftMain }}</text>
              <text v-if="item.leftSub1" class="left-sub1">{{ item.leftSub1 }}</text>
              <text v-if="item.leftSub2" class="left-sub2">{{ item.leftSub2 }}</text>
            </view>
            <!-- 底部锯齿 -->
            <view class="sawtooth-row">
              <view v-for="n in 8" :key="n" class="saw-item"></view>
            </view>
          </view>

          <!-- 右内容区 -->
          <view class="card-right" :style="{ background: item.rightBg }">
            <!-- 标签行 -->
            <view class="tag-row">
              <view
                v-for="(tag, ti) in item.tags"
                :key="ti"
                class="tag"
                :style="{ background: tag.bg }"
              >
                <text class="tag-txt" :style="{ color: tag.color }">{{ tag.label }}</text>
              </view>
              <text class="card-arrow">›</text>
            </view>

            <!-- 名称描述 -->
            <text class="card-name">{{ item.name }}</text>
            <text class="card-desc">{{ item.desc }}</text>

            <!-- 地点 -->
            <view class="card-loc">
              <text class="loc-icon">📍</text>
              <text class="loc-txt">{{ item.scope }}</text>
            </view>

            <!-- 进度条 -->
            <view class="progress-row">
              <text class="remain-txt">剩余 {{ item.remaining }} 张</text>
              <view class="progress-bar-bg">
                <view
                  class="progress-bar-fill"
                  :style="{
                    width: item.usedPct + '%',
                    background: item.progressColor
                  }"
                ></view>
              </view>
              <text class="pct-txt" :style="{ color: item.progressColor }">
                已抢{{ item.usedPct }}%
              </text>
            </view>

            <!-- 有效期 -->
            <text class="expire-txt">有效期至 {{ item.expireDate }}</text>

            <!-- 底部条 -->
            <view class="card-bottom" :style="{ background: item.bottomBg }">
              <view class="bottom-left">
                <text class="bottom-icon">{{ item.bottomIcon }}</text>
                <text class="bottom-cond">{{ item.bottomCond }}</text>
                <view class="bottom-sep"></view>
                <text class="bottom-valid">{{ item.validDays }}</text>
              </view>
              <!-- ⚠️ 去掉 @click.stop，统一由卡片处理跳转 -->
              <view class="grab-btn" :style="{ color: item.progressColor }">
                <text class="grab-txt">{{ item.btnText }}</text>
              </view>
            </view>
          </view>
        </view>

        <!-- 空状态 -->
        <view v-if="showList.length === 0" class="empty-box">
          <text class="empty-icon">🎫</text>
          <text class="empty-txt">暂无相关优惠券</text>
        </view>

        <view class="list-end">
          <text class="list-end-txt">— 已加载全部 —</text>
        </view>
      </view>
    </scroll-view>

  </view>
</template>

<script>
export default {
  name: 'CouponList',
  data() {
    return {
      statusBarH: 0,
      keyword: '',
      activeTab: 0,
      refreshing: false,

      tabs: [
        { label: '全部',  value: 'all'    },
        { label: '通用券', value: 'common' },
        { label: '快充券', value: 'fast'   },
        { label: '超充券', value: 'super'  },
        { label: '慢充券', value: 'slow'   },
      ],

      allList: [
        {
          id: 1,
          tabType: 'fast',
          leftBg: '#FF7043',
          leftMain: '8折',
          leftSub1: '原价¥29.9',
          leftSub2: '满50元可用',
          rightBg: '#FFF8F5',
          name: '超级快充折扣券',
          desc: '直流快充专享优惠',
          scope: '全城快充站通用',
          tags: [
            { label: '热门', bg: '#FF7043', color: '#fff' },
            { label: '限时', bg: '#FFE0CC', color: '#FF7043' },
          ],
          remaining: 172,
          usedPct: 66,
          progressColor: '#FF7043',
          expireDate: '2025-07-31',
          bottomBg: '#FF7043',
          bottomIcon: '⚡',
          bottomCond: '最低布 20 度',
          validDays: '30天有效',
          btnText: '立即领取',
          originalPrice: 29.9,
          price: 0,
          isFree: true,
          discountType: 'percent',
          discountValue: '8',
          detail: '本券适用于直流快充桩，每人限领一张，不可叠加其他优惠。',
        },
        {
          id: 2,
          tabType: 'common',
          leftBg: '#4CAF50',
          leftMain: '免费领',
          leftSub1: '',
          leftSub2: '满30元可用',
          rightBg: '#F5FFF6',
          name: '新用户专享券',
          desc: '注册即享，充电立减',
          scope: '全平台通用',
          tags: [
            { label: '新人',  bg: '#4CAF50', color: '#fff'     },
            { label: '免费领', bg: '#E8F5E9', color: '#4CAF50' },
          ],
          remaining: 328,
          usedPct: 67,
          progressColor: '#4CAF50',
          expireDate: '2025-06-30',
          bottomBg: '#4CAF50',
          bottomIcon: '🎁',
          bottomCond: '最低充 10 度',
          validDays: '7天有效',
          btnText: '立即领取',
          originalPrice: 0,
          price: 0,
          isFree: true,
          discountType: 'amount',
          discountValue: '5',
          detail: '新用户注册后即可免费领取，有效期7天，每人限领一张。',
        },
        {
          id: 3,
          tabType: 'super',
          leftBg: '#9C27B0',
          leftMain: '月卡特惠',
          leftSub1: '原价¥199',
          leftSub2: '',
          rightBg: '#FAF5FF',
          name: '月度畅充卡',
          desc: '30天无限次充电优惠',
          scope: '指定站点通用',
          tags: [
            { label: '超值', bg: '#9C27B0', color: '#fff'     },
            { label: '月卡', bg: '#EDE7F6', color: '#9C27B0' },
          ],
          remaining: 44,
          usedPct: 78,
          progressColor: '#9C27B0',
          expireDate: '2025-08-31',
          bottomBg: '#9C27B0',
          bottomIcon: '⭐',
          bottomCond: '最低充 0 度',
          validDays: '30天有效',
          btnText: '立即领取',
          originalPrice: 199,
          price: 99,
          isFree: false,
          discountType: 'amount',
          discountValue: '100',
          detail: '月度畅充卡，购买后30天内可在指定站点无限次充电。',
        },
        {
          id: 4,
          tabType: 'slow',
          leftBg: '#1565C0',
          leftMain: '5折封顶',
          leftSub1: '原价¥19.9',
          leftSub2: '满20元可用',
          rightBg: '#F0F4FF',
          name: '深夜低谷充电券',
          desc: '23:00-07:00专属优惠',
          scope: '夜间充电专享',
          tags: [
            { label: '低谷时段', bg: '#1565C0', color: '#fff'     },
            { label: '限时',    bg: '#DDEEFF', color: '#1565C0' },
          ],
          remaining: 99,
          usedPct: 67,
          progressColor: '#1565C0',
          expireDate: '2025-07-15',
          bottomBg: '#1565C0',
          bottomIcon: '⭐',
          bottomCond: '最低充 15 度',
          validDays: '15天有效',
          btnText: '立即领取',
          originalPrice: 19.9,
          price: 9.9,
          isFree: false,
          discountType: 'percent',
          discountValue: '5',
          detail: '每日23:00-07:00使用，享受5折优惠，最高优惠封顶，夜间充电必备。',
        },
        {
          id: 5,
          tabType: 'super',
          leftBg: '#FF5722',
          leftMain: '超充',
          leftSub1: '闪电特惠',
          leftSub2: '满60元可用',
          rightBg: '#FFF3EE',
          name: '超充闪电券',
          desc: '超充桩专属极速特惠',
          scope: '超充站点专用',
          tags: [
            { label: '超充', bg: '#FF5722', color: '#fff'     },
            { label: '闪电', bg: '#FFE8DE', color: '#FF5722' },
          ],
          remaining: 55,
          usedPct: 55,
          progressColor: '#FF5722',
          expireDate: '2025-08-15',
          bottomBg: '#FF5722',
          bottomIcon: '⚡',
          bottomCond: '最低充 30 度',
          validDays: '20天有效',
          btnText: '立即领取',
          originalPrice: 39.9,
          price: 19.9,
          isFree: false,
          discountType: 'amount',
          discountValue: '20',
          detail: '超充桩专属优惠券，享受极速充电同时享受专属折扣，每人限购2张。',
        },
      ],

      showList: [],
    }
  },

  onLoad() {
    try {
      const info = uni.getSystemInfoSync()
      this.statusBarH = info.statusBarHeight || 0
    } catch (e) {
      this.statusBarH = 0
    }
    this.showList = [...this.allList]
  },

  methods: {
    switchTab(i) {
      this.activeTab = i
      const val = this.tabs[i].value
      this.showList = val === 'all'
        ? [...this.allList]
        : this.allList.filter(item => item.tabType === val)
    },

    onSearch() {
      const kw = this.keyword.trim()
      const base = this.activeTab === 0
        ? this.allList
        : this.allList.filter(item => item.tabType === this.tabs[this.activeTab].value)
      this.showList = kw
        ? base.filter(item =>
            item.name.includes(kw) || item.desc.includes(kw)
          )
        : [...base]
    },

    onRefresh() {
      this.refreshing = true
      setTimeout(() => {
        this.refreshing = false
        uni.showToast({ title: '刷新成功', icon: 'success' })
      }, 1000)
    },

    // ★ 核心跳转方法 ★
    goDetail(item) {
      // 防止 item 为空
      if (!item || !item.id) {
        uni.showToast({ title: '数据异常', icon: 'none' })
        return
      }

      // 存储完整数据
      try {
        uni.setStorageSync('coupon_detail', JSON.stringify(item))
      } catch (e) {
        console.error('存储失败', e)
      }

      // 跳转 —— 路径必须与 pages.json 完全一致
      uni.navigateTo({
        url: '/pages/coupon-detail/coupon-detail?id=' + item.id,
        success: () => {
          console.log('跳转成功')
        },
        fail: (err) => {
          console.error('跳转失败', err)
          uni.showToast({ title: '页面跳转失败：' + err.errMsg, icon: 'none' })
        },
      })
    },
  },
}
</script>

<style>
page { background: #f2f3f7; }

.page {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background: #f2f3f7;
}

/* 导航栏 */
.nav-bar {
  background: linear-gradient(135deg, #2ecc71, #27ae60);
  padding-left: 30rpx;
  padding-right: 30rpx;
  padding-bottom: 24rpx;
}
.nav-main {
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  align-items: center;
  padding-top: 16rpx;
  margin-bottom: 20rpx;
}
.nav-title {
  font-size: 36rpx;
  font-weight: 800;
  color: #fff;
  display: block;
}
.nav-sub {
  font-size: 24rpx;
  color: rgba(255,255,255,0.8);
  display: block;
}
.nav-icon-wrap {
  position: relative;
  width: 80rpx;
  height: 80rpx;
  background: rgba(255,255,255,0.2);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}
.nav-icon-txt { font-size: 36rpx; }
.nav-badge {
  position: absolute;
  top: -4rpx;
  right: -4rpx;
  width: 32rpx;
  height: 32rpx;
  background: #ff4444;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}
.nav-badge-text { font-size: 20rpx; color: #fff; font-weight: 700; }

/* 搜索框 */
.search-box {
  display: flex;
  flex-direction: row;
  align-items: center;
  background: rgba(255,255,255,0.22);
  border-radius: 40rpx;
  padding: 14rpx 24rpx;
  gap: 12rpx;
}
.search-icon-txt { font-size: 26rpx; }
.search-input { flex: 1; font-size: 28rpx; color: #fff; }

/* Tab */
.tab-wrap {
  background: #fff;
  border-bottom: 1rpx solid #eee;
}
.tab-scroll { white-space: nowrap; }
.tab-list {
  display: flex;
  flex-direction: row;
  padding: 0 10rpx;
}
.tab-item {
  padding: 22rpx 30rpx 0;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-bottom: 16rpx;
  position: relative;
}
.tab-txt { font-size: 28rpx; color: #888; }
.tab-txt-active { color: #27ae60; font-weight: 700; }
.tab-line {
  position: absolute;
  bottom: 0;
  width: 36rpx;
  height: 4rpx;
  background: #27ae60;
  border-radius: 2rpx;
}

/* 统计栏 */
.stat-bar {
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  align-items: center;
  padding: 16rpx 30rpx;
  background: #fff;
  border-top: 1rpx solid #f0f0f0;
  margin-bottom: 16rpx;
}
.stat-txt { font-size: 26rpx; color: #666; }
.stat-num { color: #27ae60; font-weight: 700; }
.stat-link { font-size: 26rpx; color: #27ae60; }

/* 列表 */
.list-scroll { flex: 1; }
.list-wrap { padding: 0 24rpx 30rpx; }

/* 卡片 */
.coupon-card {
  display: flex;
  flex-direction: row;
  border-radius: 20rpx;
  overflow: hidden;
  margin-bottom: 24rpx;
  box-shadow: 0 4rpx 20rpx rgba(0,0,0,0.10);
  /* ⚠️ 确保可点击 */
  position: relative;
}

/* 左色块 */
.card-left {
  width: 196rpx;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: space-between;
  overflow: hidden;
}
.sawtooth-row {
  display: flex;
  flex-direction: row;
  justify-content: space-around;
  width: 100%;
  padding: 0 4rpx;
}
.saw-item {
  width: 18rpx;
  height: 18rpx;
  border-radius: 50%;
  background: #f2f3f7;
  flex-shrink: 0;
}
.left-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 16rpx 12rpx;
}
.left-main {
  font-size: 50rpx;
  font-weight: 900;
  color: #fff;
  text-align: center;
  line-height: 1.1;
}
.left-sub1 {
  font-size: 22rpx;
  color: rgba(255,255,255,0.85);
  margin-top: 6rpx;
  text-align: center;
  text-decoration: line-through;
}
.left-sub2 {
  font-size: 22rpx;
  color: rgba(255,255,255,0.95);
  margin-top: 4rpx;
  text-align: center;
}

/* 右侧 */
.card-right {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.tag-row {
  display: flex;
  flex-direction: row;
  align-items: center;
  padding: 16rpx 16rpx 6rpx;
  gap: 8rpx;
}
.tag {
  padding: 4rpx 14rpx;
  border-radius: 20rpx;
}
.tag-txt { font-size: 22rpx; font-weight: 600; }
.card-arrow { margin-left: auto; font-size: 36rpx; color: #ccc; }
.card-name {
  font-size: 30rpx;
  font-weight: 800;
  color: #222;
  padding: 0 16rpx 4rpx;
  display: block;
}
.card-desc {
  font-size: 24rpx;
  color: #888;
  padding: 0 16rpx 8rpx;
  display: block;
}
.card-loc {
  display: flex;
  flex-direction: row;
  align-items: center;
  padding: 0 16rpx 10rpx;
  gap: 4rpx;
}
.loc-icon { font-size: 22rpx; }
.loc-txt  { font-size: 24rpx; color: #888; }
.progress-row {
  display: flex;
  flex-direction: row;
  align-items: center;
  padding: 0 16rpx 8rpx;
  gap: 8rpx;
}
.remain-txt { font-size: 22rpx; color: #aaa; white-space: nowrap; }
.progress-bar-bg {
  flex: 1;
  height: 10rpx;
  background: #eee;
  border-radius: 5rpx;
  overflow: hidden;
}
.progress-bar-fill { height: 100%; border-radius: 5rpx; }
.pct-txt { font-size: 22rpx; white-space: nowrap; font-weight: 600; }
.expire-txt {
  font-size: 22rpx;
  color: #bbb;
  padding: 0 16rpx 10rpx;
  display: block;
}
.card-bottom {
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: space-between;
  padding: 14rpx 16rpx;
  margin-top: auto;
}
.bottom-left {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 8rpx;
  flex: 1;
}
.bottom-icon  { font-size: 24rpx; }
.bottom-cond  { font-size: 22rpx; color: rgba(255,255,255,0.9); }
.bottom-sep   { width: 2rpx; height: 24rpx; background: rgba(255,255,255,0.4); margin: 0 4rpx; }
.bottom-valid { font-size: 22rpx; color: rgba(255,255,255,0.9); }
.grab-btn {
  background: #fff;
  padding: 10rpx 22rpx;
  border-radius: 30rpx;
  flex-shrink: 0;
}
.grab-txt { font-size: 24rpx; font-weight: 700; }

/* 空/底部 */
.empty-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 100rpx 0;
  gap: 20rpx;
}
.empty-icon { font-size: 80rpx; }
.empty-txt  { font-size: 30rpx; color: #999; }
.list-end   { text-align: center; padding: 30rpx 0 20rpx; }
.list-end-txt { font-size: 26rpx; color: #ccc; }
</style>
```

---

## 最常见的跳转失败原因速查

```
❌ pages.json 未注册 coupon-detail 页面
   → 添加 "path": "pages/coupon/coupon-detail"

❌ 路径大小写错误（Windows不报错，真机报错）
   → 文件名和路径必须完全一致

❌ grab-btn 上有 @click.stop 阻止了冒泡
   → 去掉 .stop 修饰符，或在按钮上直接绑定跳转

❌ item 数据为空触发了 return
   → 在 goDetail 中加 console.log(item) 确认数据

❌ 使用了 uni.switchTab 但目标页不是 tabBar 页
   → 改用 uni.navigateTo
```
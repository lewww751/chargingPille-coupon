<template>
  <view class="detail-page">

    <!-- 顶部英雄区 -->
    <view class="hero" :style="{ background: coupon.leftBg || '#27ae60' }">
      <!-- 装饰圆 -->
      <view class="hero-circle c1"></view>
      <view class="hero-circle c2"></view>
      <view class="hero-circle c3"></view>

      <view class="hero-inner">
        <!-- 标签 -->
        <view class="hero-tags">
          <view
            v-for="(tag, i) in (coupon.tags || [])"
            :key="i"
            class="hero-tag"
          >
            <text class="hero-tag-txt">{{ tag.label }}</text>
          </view>
        </view>

        <!-- 大面值 -->
        <view class="hero-discount">
          <text class="hero-main">{{ coupon.leftMain }}</text>
        </view>
        <text v-if="coupon.leftSub2" class="hero-cond">{{ coupon.leftSub2 }}</text>
        <text class="hero-name">{{ coupon.name }}</text>
        <text class="hero-desc">{{ coupon.desc }}</text>

        <!-- 剩余进度 -->
        <view class="hero-progress">
          <view class="hp-row">
            <text class="hp-remain">剩余 {{ coupon.remaining }} 张</text>
            <text class="hp-pct">已抢 {{ coupon.usedPct }}%</text>
          </view>
          <view class="hp-bar">
            <view class="hp-fill" :style="{ width: coupon.usedPct + '%' }"></view>
          </view>
        </view>
      </view>
    </view>

    <!-- 内容滚动 -->
    <scroll-view class="detail-scroll" scroll-y>

      <!-- 基本信息卡 -->
      <view class="d-card">
        <view class="d-card-title-row">
          <view class="d-title-bar" :style="{ background: coupon.progressColor || '#27ae60' }"></view>
          <text class="d-card-title">券信息</text>
        </view>
        <view class="d-grid">
          <view class="d-cell">
            <text class="d-label">优惠类型</text>
            <text class="d-val">{{ coupon.discountType === 'percent' ? '折扣券' : '满减券' }}</text>
          </view>
          <view class="d-cell">
            <text class="d-label">优惠力度</text>
            <text class="d-val d-highlight">
              {{ coupon.discountType === 'percent' ? coupon.discountValue + '折优惠' : '立减¥' + coupon.discountValue }}
            </text>
          </view>
          <view class="d-cell">
            <text class="d-label">使用门槛</text>
            <text class="d-val">{{ coupon.leftSub2 || '无门槛' }}</text>
          </view>
          <view class="d-cell">
            <text class="d-label">适用范围</text>
            <text class="d-val">{{ coupon.scope }}</text>
          </view>
          <view class="d-cell">
            <text class="d-label">有效期至</text>
            <text class="d-val">{{ coupon.expireDate }}</text>
          </view>
          <view class="d-cell">
            <text class="d-label">有效天数</text>
            <text class="d-val">{{ coupon.validDays }}</text>
          </view>
          <view class="d-cell">
            <text class="d-label">最低用电</text>
            <text class="d-val">{{ coupon.bottomCond }}</text>
          </view>
          <view class="d-cell">
            <text class="d-label">剩余数量</text>
            <text class="d-val">{{ coupon.remaining }} 张</text>
          </view>
        </view>
      </view>

      <!-- 活动说明 -->
      <view class="d-card">
        <view class="d-card-title-row">
          <view class="d-title-bar" :style="{ background: coupon.progressColor || '#27ae60' }"></view>
          <text class="d-card-title">活动说明</text>
        </view>
        <text class="d-desc-txt">{{ coupon.detail || coupon.desc }}</text>
      </view>

      <!-- 使用规则 -->
      <view class="d-card">
        <view class="d-card-title-row">
          <view class="d-title-bar" :style="{ background: coupon.progressColor || '#27ae60' }"></view>
          <text class="d-card-title">使用规则</text>
        </view>
        <view v-for="(r, i) in rules" :key="i" class="d-rule-item">
          <view class="d-rule-dot" :style="{ background: coupon.progressColor || '#27ae60' }"></view>
          <text class="d-rule-txt">{{ r }}</text>
        </view>
      </view>

      <!-- 适用充电站 -->
      <view class="d-card">
        <view class="d-card-title-row">
          <view class="d-title-bar" :style="{ background: coupon.progressColor || '#27ae60' }"></view>
          <text class="d-card-title">适用充电站</text>
          <text
            class="d-card-more"
            :style="{ color: coupon.progressColor || '#27ae60' }"
            @click="viewAllStations"
          >查看全部 ›</text>
        </view>
        <view
          v-for="(st, i) in stations"
          :key="i"
          class="d-station"
          @click="goStation(st)"
        >
          <view class="d-st-icon-wrap" :style="{ background: getTagBg(coupon.progressColor) }">
            <text class="d-st-icon">⚡</text>
          </view>
          <view class="d-st-info">
            <text class="d-st-name">{{ st.name }}</text>
            <text class="d-st-addr">{{ st.address }}</text>
          </view>
          <text class="d-st-dist">{{ st.distance }}</text>
          <text class="d-st-arrow">›</text>
        </view>
      </view>

      <view style="height:220rpx;"></view>
    </scroll-view>

    <!-- 底部操作栏 -->
    <view class="detail-bottom">
      <!-- 价格区 -->
      <view class="db-price">
        <text v-if="coupon.isFree" class="db-free">免费领取</text>
        <view v-else class="db-price-row">
          <text class="db-cur">¥{{ coupon.price }}</text>
          <text class="db-ori">¥{{ coupon.originalPrice }}</text>
        </view>
        <text class="db-expire">{{ coupon.expireDate }} 到期</text>
      </view>

      <!-- 按钮区 -->
      <view class="db-btns">
        <!-- 收藏 -->
        <view class="db-collect" @click="onCollect">
          <text class="db-collect-icon">{{ collected ? '❤️' : '🤍' }}</text>
          <text class="db-collect-txt" :style="{ color: collected ? '#ff4444' : '#999' }">收藏</text>
        </view>

        <!-- ★ 购买/领取按钮 — 预留跳转 ★ -->
        <view
          class="db-buy-btn"
          :style="{ background: canBuy ? (coupon.leftBg || '#27ae60') : '#ccc' }"
          @click="onBuyClick"
        >
          <text class="db-buy-txt">{{ buyText }}</text>
        </view>
      </view>
    </view>

    <!-- 购买确认弹窗 -->
    <view v-if="showModal" class="modal-mask" @click.self="showModal = false">
      <view class="modal-box">

        <!-- 弹窗头 -->
        <view class="mb-head">
          <text class="mb-title">确认{{ coupon.isFree ? '领取' : '购买' }}</text>
          <text class="mb-close" @click="showModal = false">✕</text>
        </view>

        <!-- 券预览 -->
        <view class="mb-preview">
          <view class="mb-pre-left" :style="{ background: coupon.leftBg || '#27ae60' }">
            <text class="mb-pre-main">{{ coupon.leftMain }}</text>
            <text class="mb-pre-cond">{{ coupon.leftSub2 }}</text>
          </view>
          <view class="mb-pre-right">
            <text class="mb-pre-name">{{ coupon.name }}</text>
            <text class="mb-pre-expire">有效期至 {{ coupon.expireDate }}</text>
            <text class="mb-pre-price">
              {{ coupon.isFree ? '免费' : '¥' + coupon.price }}
            </text>
          </view>
        </view>

        <!-- 支付方式 -->
        <text class="mb-pay-title">选择支付方式</text>
        <view
          v-for="m in payMethods"
          :key="m.id"
          class="mb-pay-item"
          :class="{ 'mb-pay-active': selPay === m.id }"
          @click="selPay = m.id"
        >
          <text class="mb-pay-icon">{{ m.icon }}</text>
          <text class="mb-pay-name">{{ m.name }}</text>
          <view v-if="selPay === m.id" class="mb-pay-check">
            <text class="mb-check-txt">✓</text>
          </view>
        </view>

        <!-- 协议 -->
        <view class="mb-agree-row">
          <view
            class="mb-agree-box"
            :class="{ 'mb-agree-checked': agreed }"
            :style="agreed ? { background: coupon.progressColor || '#27ae60', borderColor: coupon.progressColor || '#27ae60' } : {}"
            @click="agreed = !agreed"
          >
            <text v-if="agreed" class="mb-agree-icon">✓</text>
          </view>
          <text class="mb-agree-txt">我已阅读并同意</text>
          <text
            class="mb-agree-link"
            :style="{ color: coupon.progressColor || '#27ae60' }"
            @click="viewTerms"
          >《优惠券使用协议》</text>
        </view>

        <!-- ★ 确认支付按钮 — 核心跳转预留 ★ -->
        <view
          class="mb-confirm-btn"
          :style="{ background: agreed ? (coupon.leftBg || '#27ae60') : '#ccc' }"
          @click="onConfirmPay"
        >
          <text class="mb-confirm-txt">
            {{ coupon.isFree ? '立即领取' : '确认支付 ¥' + coupon.price }}
          </text>
        </view>

      </view>
    </view>

  </view>
</template>

<script>
export default {
  name: 'CouponDetail',
  data() {
    return {
      coupon: {},
      collected: false,
      agreed: false,
      selPay: 'wechat',
      showModal: false,

      payMethods: [
        { id: 'wechat', name: '积分支付', icon: '💚' },
        // { id: 'alipay', name: '支付宝',   icon: '💙' },
        { id: 'wallet', name: '账户余额',  icon: '💰' },
      ],

      rules: [
        '每位用户每种券限购/领取一张',
        '券有效期内随时可用，过期自动失效',
        '本券不可转让，不可兑换现金',
        '不可与其他优惠活动叠加使用',
        '如有退款，优惠券退回原渠道',
        '本平台保留最终解释权',
      ],

      stations: [
        { id: 1, name: '朝阳区望京充电中心',   address: '北京市朝阳区望京街道',     distance: '0.8km' },
        { id: 2, name: '海淀区中关村超充站',   address: '北京市海淀区中关村南大街', distance: '1.2km' },
        { id: 3, name: '西城区金融街充电站',   address: '北京市西城区金融街15号',   distance: '2.3km' },
        { id: 4, name: '东城区王府井快充站',   address: '北京市东城区王府井大街',   distance: '3.1km' },
      ],
    }
  },

  computed: {
    canBuy() {
      return this.coupon.remaining > 0
    },
    buyText() {
      if (!this.coupon.remaining) return '已售罄'
      return this.coupon.isFree ? '免费领取' : '立即购买'
    },
  },

  onLoad(opts) {
    try {
      const raw = uni.getStorageSync('coupon_detail')
      if (raw) this.coupon = JSON.parse(raw)
    } catch (e) {}
    // TODO: 也可通过接口获取 → this.fetchDetail(opts.id)
  },

  methods: {
    // 颜色辅助
    getTagBg(color) {
      return color ? color + '22' : '#27ae6022'
    },

    // 收藏
    onCollect() {
      this.collected = !this.collected
      uni.showToast({ title: this.collected ? '收藏成功 ❤️' : '已取消收藏', icon: 'none' })
      // TODO: api.toggleCollect({ id: this.coupon.id, collected: this.collected })
    },

    // ★ 购买按钮点击入口 ★
    onBuyClick() {
      if (!this.canBuy) {
        uni.showToast({ title: '该券已售罄', icon: 'none' })
        return
      }
      // 检查登录态
      // const token = uni.getStorageSync('user_token')
      // if (!token) {
      //   uni.showModal({
      //     title: '请先登录',
      //     content: '登录后即可领取/购买优惠券',
      //     confirmText: '去登录',
      //     success: (res) => {
      //       if (res.confirm) {
      //         // ============================
      //         // ★ 预留跳转：登录页
      //         // ============================
      //         uni.navigateTo({ url: '/pages/user/login' })
      //       }
      //     },
      //   })
      //   return
      // }
      // 打开确认弹窗
       this.agreed = false
       this.showModal = true
    },

    // ★ 确认支付 — 购买核心逻辑预留 ★
    async onConfirmPay() {
      if (!this.agreed) {
        uni.showToast({ title: '请先同意协议', icon: 'none' })
        return
      }

      uni.showLoading({ title: '处理中...', mask: true })

      try {
        if (this.coupon.isFree) {
          // ============================================
          // ★ 预留：免费领取接口
          // const res = await api.claimCoupon({
          //   couponId: this.coupon.id
          // })
          // ============================================
          await this._mock(1000)
          uni.hideLoading()
          this.showModal = false
          uni.showToast({ title: '领取成功 🎉', icon: 'success' })
          setTimeout(() => {
            // ============================
            // ★ 预留跳转：我的优惠券
            // ============================
            uni.switchTab({ url: '/pages/user/index' })
            // 或 uni.navigateTo({ url: '/pages/user/my-coupons' })
          }, 1500)

        } else {
          // ============================================
          // ★ 预留：创建订单
          // const order = await api.createOrder({
          //   couponId:  this.coupon.id,
          //   payMethod: this.selPay,
          //   price:     this.coupon.price,
          // })
          //
          // ★ 预留：调起支付
          // await this._invokePay(order.orderNo, this.selPay)
          // ============================================
          await this._mock(1500)
          uni.hideLoading()
          this.showModal = false
          // ============================
          // ★ 预留跳转：支付成功页
          // ============================
          uni.navigateTo({
            url: `/pages/order/pay-success?couponId=${this.coupon.id}&name=${encodeURIComponent(this.coupon.name)}`,
          })
        }

      } catch (err) {
        uni.hideLoading()
        console.error('购买失败', err)
        uni.showToast({ title: err.message || '操作失败，请重试', icon: 'none' })
        // ============================
        // ★ 预留跳转：支付失败页
        // ============================
        // uni.navigateTo({ url: '/pages/order/pay-fail' })
      }
    },

    // 调起平台支付（预留实现）
    async _invokePay(orderNo, method) {
      // ============================
      // ★ 预留：微信/支付宝/余额支付
      // ============================
      // if (method === 'wechat') { ... uni.requestPayment ... }
      // if (method === 'alipay') { ... uni.requestPayment ... }
      // if (method === 'wallet') { ... api.payWithWallet ... }
    },

    // 查看全部充电站
    viewAllStations() {
      // ============================
      // ★ 预留跳转：充电站列表
      // ============================
      uni.navigateTo({ url: `/pages/station/list?couponId=${this.coupon.id}` })
    },

    // 跳转充电站详情
    goStation(st) {
      // ============================
      // ★ 预留跳转：充电站详情
      // ============================
      uni.navigateTo({ url: `/pages/station/detail?id=${st.id}` })
    },

    // 查看协议
    viewTerms() {
      uni.navigateTo({ url: '/pages/common/agreement?type=coupon' })
    },

    // 模拟请求延迟
    _mock(ms) {
      return new Promise(r => setTimeout(r, ms))
    },
  },
}
</script>

<style>
/* ─── 详情页 ─── */
.detail-page {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background: #f2f3f7;
}

/* 英雄区 */
.hero {
  position: relative;
  overflow: hidden;
  padding: 80rpx 36rpx 50rpx;
}
.hero-circle {
  position: absolute;
  border-radius: 50%;
  background: rgba(255,255,255,0.10);
}
.c1 { width: 400rpx; height: 400rpx; top: -120rpx; right: -80rpx; }
.c2 { width: 240rpx; height: 240rpx; bottom: -80rpx; left: -60rpx; }
.c3 { width: 150rpx; height: 150rpx; top: 60rpx; left: 55%; }

.hero-inner { position: relative; z-index: 1; }

.hero-tags {
  display: flex;
  flex-direction: row;
  gap: 10rpx;
  margin-bottom: 20rpx;
}
.hero-tag {
  background: rgba(255,255,255,0.28);
  border-radius: 20rpx;
  padding: 4rpx 18rpx;
}
.hero-tag-txt { font-size: 24rpx; color: #fff; font-weight: 600; }

.hero-discount { margin-bottom: 8rpx; }
.hero-main {
  font-size: 90rpx;
  font-weight: 900;
  color: #fff;
  line-height: 1;
  display: block;
}
.hero-cond {
  font-size: 28rpx;
  color: rgba(255,255,255,0.82);
  display: block;
  margin-bottom: 8rpx;
}
.hero-name {
  font-size: 38rpx;
  font-weight: 800;
  color: #fff;
  display: block;
  margin-bottom: 6rpx;
}
.hero-desc {
  font-size: 26rpx;
  color: rgba(255,255,255,0.8);
  display: block;
  margin-bottom: 28rpx;
}
.hero-progress {}
.hp-row {
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  margin-bottom: 10rpx;
}
.hp-remain { font-size: 26rpx; color: rgba(255,255,255,0.85); }
.hp-pct    { font-size: 26rpx; color: rgba(255,255,255,0.85); font-weight: 600; }
.hp-bar {
  height: 10rpx;
  background: rgba(255,255,255,0.28);
  border-radius: 5rpx;
  overflow: hidden;
}
.hp-fill {
  height: 100%;
  background: rgba(255,255,255,0.9);
  border-radius: 5rpx;
}

/* ─── 滚动内容 ─── */
.detail-scroll { flex: 1; }

/* 通用卡片 */
.d-card {
  background: #fff;
  border-radius: 20rpx;
  margin: 20rpx 24rpx 0;
  padding: 28rpx;
}
.d-card-title-row {
  display: flex;
  flex-direction: row;
  align-items: center;
  margin-bottom: 22rpx;
}
.d-title-bar {
  width: 6rpx;
  height: 32rpx;
  border-radius: 3rpx;
  margin-right: 12rpx;
  flex-shrink: 0;
}
.d-card-title {
  font-size: 30rpx;
  font-weight: 700;
  color: #222;
  flex: 1;
}
.d-card-more { font-size: 26rpx; }

/* 信息格 */
.d-grid {
  display: flex;
  flex-direction: row;
  flex-wrap: wrap;
}
.d-cell {
  width: 50%;
  margin-bottom: 20rpx;
  display: flex;
  flex-direction: column;
  gap: 6rpx;
}
.d-label { font-size: 24rpx; color: #aaa; }
.d-val   { font-size: 26rpx; color: #333; font-weight: 500; }
.d-highlight { color: #ff4444; font-weight: 700; }

/* 说明 */
.d-desc-txt { font-size: 28rpx; color: #555; line-height: 1.8; }

/* 规则 */
.d-rule-item {
  display: flex;
  flex-direction: row;
  align-items: flex-start;
  margin-bottom: 14rpx;
  gap: 12rpx;
}
.d-rule-dot {
  width: 12rpx;
  height: 12rpx;
  border-radius: 50%;
  margin-top: 10rpx;
  flex-shrink: 0;
}
.d-rule-txt { font-size: 26rpx; color: #555; line-height: 1.7; flex: 1; }

/* 充电站 */
.d-station {
  display: flex;
  flex-direction: row;
  align-items: center;
  padding: 20rpx 0;
  border-bottom: 1rpx solid #f5f5f5;
  gap: 16rpx;
}
.d-st-icon-wrap {
  width: 64rpx;
  height: 64rpx;
  border-radius: 14rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.d-st-icon { font-size: 30rpx; }
.d-st-info { flex: 1; }
.d-st-name {
  font-size: 28rpx;
  font-weight: 600;
  color: #222;
  display: block;
  margin-bottom: 6rpx;
}
.d-st-addr { font-size: 24rpx; color: #999; display: block; }
.d-st-dist { font-size: 24rpx; color: #aaa; }
.d-st-arrow { font-size: 36rpx; color: #ddd; margin-left: 4rpx; }

/* ─── 底部操作栏 ─── */
.detail-bottom {
  position: fixed;
  bottom: 0; left: 0; right: 0;
  background: #fff;
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: space-between;
  padding: 20rpx 30rpx 40rpx;
  box-shadow: 0 -4rpx 20rpx rgba(0,0,0,0.07);
}
.db-price {}
.db-free { font-size: 32rpx; font-weight: 700; color: #ff4444; display: block; }
.db-price-row {
  display: flex;
  flex-direction: row;
  align-items: baseline;
  gap: 10rpx;
}
.db-cur { font-size: 48rpx; font-weight: 900; color: #ff4444; }
.db-ori { font-size: 26rpx; color: #ccc; text-decoration: line-through; }
.db-expire { font-size: 22rpx; color: #bbb; display: block; margin-top: 4rpx; }

.db-btns {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 20rpx;
}
.db-collect {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4rpx;
  padding: 10rpx 14rpx;
}
.db-collect-icon { font-size: 40rpx; }
.db-collect-txt  { font-size: 22rpx; }
.db-buy-btn {
  padding: 24rpx 60rpx;
  border-radius: 50rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}
.db-buy-txt { font-size: 30rpx; color: #fff; font-weight: 700; }

/* ─── 弹窗 ─── */
.modal-mask {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.5);
  display: flex;
  align-items: flex-end;
  z-index: 999;
}
.modal-box {
  background: #fff;
  border-radius: 32rpx 32rpx 0 0;
  padding: 40rpx 30rpx 60rpx;
  width: 100%;
  box-sizing: border-box;
}
.mb-head {
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30rpx;
}
.mb-title { font-size: 34rpx; font-weight: 700; color: #222; }
.mb-close { font-size: 40rpx; color: #aaa; padding: 10rpx; }

.mb-preview {
  display: flex;
  flex-direction: row;
  gap: 20rpx;
  background: #f7f7f7;
  border-radius: 16rpx;
  padding: 20rpx;
  margin-bottom: 28rpx;
}
.mb-pre-left {
  width: 120rpx;
  height: 110rpx;
  border-radius: 12rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.mb-pre-main { font-size: 32rpx; font-weight: 900; color: #fff; display: block; text-align: center; }
.mb-pre-cond { font-size: 20rpx; color: rgba(255,255,255,0.85); display: block; text-align: center; }
.mb-pre-right { flex: 1; display: flex; flex-direction: column; justify-content: center; gap: 8rpx; }
.mb-pre-name  { font-size: 28rpx; font-weight: 700; color: #222; display: block; }
.mb-pre-expire{ font-size: 24rpx; color: #999; display: block; }
.mb-pre-price { font-size: 36rpx; font-weight: 900; color: #ff4444; display: block; }

.mb-pay-title { font-size: 28rpx; font-weight: 700; color: #222; display: block; margin-bottom: 16rpx; }
.mb-pay-item {
  display: flex;
  flex-direction: row;
  align-items: center;
  padding: 18rpx 20rpx;
  border-radius: 14rpx;
  border: 2rpx solid #eee;
  margin-bottom: 14rpx;
  gap: 16rpx;
}
.mb-pay-active { border-color: #27ae60; background: rgba(39,174,96,0.05); }
.mb-pay-icon { font-size: 36rpx; }
.mb-pay-name { flex: 1; font-size: 28rpx; color: #333; }
.mb-pay-check {
  width: 36rpx; height: 36rpx; border-radius: 50%;
  background: #27ae60;
  display: flex; align-items: center; justify-content: center;
}
.mb-check-txt { font-size: 22rpx; color: #fff; font-weight: 700; }

.mb-agree-row {
  display: flex;
  flex-direction: row;
  align-items: center;
  margin: 20rpx 0 24rpx;
  gap: 10rpx;
}
.mb-agree-box {
  width: 36rpx; height: 36rpx;
  border-radius: 8rpx;
  border: 2rpx solid #ddd;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.mb-agree-icon { font-size: 22rpx; color: #fff; font-weight: 700; }
.mb-agree-txt  { font-size: 24rpx; color: #999; }
.mb-agree-link { font-size: 24rpx; }

.mb-confirm-btn {
  border-radius: 50rpx;
  padding: 28rpx;
  text-align: center;
}
.mb-confirm-txt { font-size: 32rpx; color: #fff; font-weight: 700; }
</style>
package com.campus.trade.service;

import com.campus.trade.common.BusinessException;
import com.campus.trade.common.PasswordCodec;
import com.campus.trade.mapper.FavoriteMapper;
import com.campus.trade.mapper.HistoryMapper;
import com.campus.trade.mapper.ItemMapper;
import com.campus.trade.mapper.MessageMapper;
import com.campus.trade.mapper.OrderMapper;
import com.campus.trade.mapper.ReportMapper;
import com.campus.trade.mapper.UserMapper;
import com.campus.trade.mapper.WantedMapper;
import com.campus.trade.security.JwtTokenProvider;
import com.campus.trade.security.LoginUser;
import com.campus.trade.security.UserContext;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DemoDataService {
    private static final int ORDER_PAYMENT_TIMEOUT_MINUTES = 30;
    private static final String ORDER_TIMEOUT_REASON = "订单超时未支付，系统已自动关闭";
    private static final List<String> SUPPORTED_CAMPUSES = List.of("望江校区", "江安校区");
    private static final List<String> SUPPORTED_LOGIN_TYPES = List.of("NICKNAME", "STUDENT_NO", "ADMIN");
    private static final List<String> SUPPORTED_PAYMENT_METHODS = List.of("WECHAT", "ALIPAY", "CAMPUS_CARD");
    private final UserMapper userMapper;
    private final ItemMapper itemMapper;
    private final MessageMapper messageMapper;
    private final OrderMapper orderMapper;
    private final FavoriteMapper favoriteMapper;
    private final WantedMapper wantedMapper;
    private final ReportMapper reportMapper;
    private final HistoryMapper historyMapper;
    private final JwtTokenProvider jwtTokenProvider;

    public DemoDataService(
            UserMapper userMapper,
            ItemMapper itemMapper,
            MessageMapper messageMapper,
            OrderMapper orderMapper,
            FavoriteMapper favoriteMapper,
            WantedMapper wantedMapper,
            ReportMapper reportMapper,
            HistoryMapper historyMapper,
            JwtTokenProvider jwtTokenProvider
    ) {
        this.userMapper = userMapper;
        this.itemMapper = itemMapper;
        this.messageMapper = messageMapper;
        this.orderMapper = orderMapper;
        this.favoriteMapper = favoriteMapper;
        this.wantedMapper = wantedMapper;
        this.reportMapper = reportMapper;
        this.historyMapper = historyMapper;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public List<Map<String, Object>> getItems(String keyword, String category, String campus) {
        String normalizedKeyword = keyword == null ? null : keyword.trim();
        String normalizedCategory = category == null ? null : category.trim();
        String normalizedCampus = normalizeCampus(campus, false);
        return itemMapper.findItems(normalizedKeyword, normalizedCategory, normalizedCampus);
    }

    public Map<String, Object> getItemDetail(Long itemId) {
        Map<String, Object> item = itemMapper.findItemDetail(itemId);
        if (item == null) {
            throw new BusinessException("ITEM_NOT_FOUND", "商品不存在");
        }
        String status = String.valueOf(item.get("status"));
        if ("OFF_SHELF".equalsIgnoreCase(status) || "DELETED".equalsIgnoreCase(status)
                || "REJECTED".equalsIgnoreCase(status) || "PENDING_REVIEW".equalsIgnoreCase(status)) {
            throw new BusinessException("ITEM_NOT_FOUND", "商品已下架或不存在");
        }
        return item;
    }

    @Transactional
    public Map<String, Object> createItem(String title, String category, BigDecimal price, Integer stock, String description, List<String> imageUrls) {
        Long sellerId = ensureActiveUserId();
        ensureCampusVerified(sellerId);
        if (stock == null || stock < 1) {
            throw new BusinessException("ITEM_STOCK_INVALID", "库存至少为 1");
        }
        Long categoryId = itemMapper.findCategoryIdByName(category);
        if (categoryId == null) {
            Map<String, Object> categoryData = new LinkedHashMap<>();
            categoryData.put("name", category);
            itemMapper.insertCategory(categoryData);
            categoryId = toLong(categoryData.get("id"));
        }

        Map<String, Object> item = new LinkedHashMap<>();
        item.put("sellerId", sellerId);
        item.put("categoryId", categoryId);
        item.put("title", title);
        item.put("description", description);
        item.put("price", price);
        item.put("stock", stock);
        itemMapper.insertItem(item);
        List<String> savedImageUrls = saveItemImages(toLong(item.get("id")), imageUrls);
        item.put("category", category);
        item.put("status", "ON_SALE");
        item.put("stock", stock);
        item.put("createdAt", OffsetDateTime.now().toString());
        item.put("imageUrls", savedImageUrls);
        item.put("imageUrl", savedImageUrls.isEmpty() ? null : savedImageUrls.getFirst());
        return item;
    }

    public List<Map<String, Object>> getOrders(String status) {
        Long userId = ensureActiveUserId();
        expireTimedOutOrders();
        orderMapper.hideInvalidOrdersForUser(userId);
        return orderMapper.findOrders(userId, status).stream()
                .map(this::appendOrderDisplayFields)
                .toList();
    }

    @Transactional
    public Map<String, Object> createOrder(
            Long itemId,
            String receiverName,
            String receiverPhone,
            String receiverCampus,
            String receiverDetail
    ) {
        Long buyerId = ensureActiveUserId();
        expireTimedOutOrders();
        ensureCampusVerified(buyerId);
        String normalizedReceiverName = normalizeRequiredField(receiverName, "ORDER_RECEIVER_REQUIRED", "请填写收货人");
        String normalizedReceiverPhone = normalizePhone(receiverPhone);
        String normalizedReceiverCampus = normalizeCampus(receiverCampus, true);
        String normalizedReceiverDetail = normalizeRequiredField(receiverDetail, "ORDER_ADDRESS_REQUIRED", "请填写详细收货地址");
        Map<String, Object> item = itemMapper.findItemBase(itemId);
        if (item == null) {
            throw new BusinessException("ITEM_NOT_FOUND", "商品不存在");
        }
        if (buyerId.equals(toLong(item.get("sellerId")))) {
            throw new BusinessException("CANNOT_ORDER_OWN_ITEM", "不能下单自己发布的商品");
        }
        String itemStatus = String.valueOf(item.get("status"));
        if (!"ON_SALE".equalsIgnoreCase(itemStatus)) {
            throw new BusinessException("ITEM_NOT_ON_SALE", "商品当前不可下单");
        }
        Integer stock = toInteger(item.get("stock"));
        if (stock == null || stock < 1) {
            throw new BusinessException("ITEM_SOLD_OUT", "商品库存不足");
        }
        if (orderMapper.countActiveOrdersByItemAndBuyer(itemId, buyerId) > 0) {
            throw new BusinessException("DUPLICATE_ORDER", "你已经下单过该商品");
        }
        if (itemMapper.decreaseStockForOrder(itemId) == 0) {
            throw new BusinessException("ITEM_SOLD_OUT", "商品库存不足，请刷新后重试");
        }
        Map<String, Object> order = new LinkedHashMap<>();
        order.put("itemId", itemId);
        order.put("buyerId", buyerId);
        order.put("sellerId", toLong(item.get("sellerId")));
        order.put("amount", item.get("price"));
        order.put("receiverName", normalizedReceiverName);
        order.put("receiverPhone", normalizedReceiverPhone);
        order.put("receiverCampus", normalizedReceiverCampus);
        order.put("receiverDetail", normalizedReceiverDetail);
        orderMapper.insertOrder(order);
        order.put("status", "PENDING_PAYMENT");
        order.put("createdAt", OffsetDateTime.now().toString());
        return appendOrderDisplayFields(order);
    }

    @Transactional
    public Map<String, Object> payOrder(Long orderId, String paymentMethod) {
        Long buyerId = ensureActiveUserId();
        expireTimedOutOrders();
        String normalizedPaymentMethod = normalizePaymentMethod(paymentMethod);
        Map<String, Object> order = orderMapper.findOrderBase(orderId);
        if (order == null) {
            throw new BusinessException("ORDER_NOT_FOUND", "订单不存在");
        }
        if (!buyerId.equals(toLong(order.get("buyerId")))) {
            throw new BusinessException("ORDER_FORBIDDEN", "仅买家可支付该订单");
        }
        if (!"PENDING_PAYMENT".equalsIgnoreCase(safeText(order.get("status")))) {
            throw new BusinessException("ORDER_STATUS_INVALID", "当前订单不可支付");
        }
        String paymentNo = buildPaymentNo(orderId);
        if (orderMapper.payOrder(orderId, normalizedPaymentMethod, paymentNo) == 0) {
            throw new BusinessException("ORDER_STATUS_INVALID", "订单支付失败，请刷新后重试");
        }
        appendSystemMessage(
                orderId,
                toLong(order.get("itemId")),
                buyerId,
                toLong(order.get("sellerId")),
                "TEXT",
                "买家已完成支付，请卖家尽快发货。"
        );
        return appendOrderDisplayFields(orderMapper.findOrderBase(orderId));
    }

    @Transactional
    public Map<String, Object> shipOrder(Long orderId) {
        Long sellerId = ensureActiveUserId();
        Map<String, Object> order = orderMapper.findOrderBase(orderId);
        if (order == null) {
            throw new BusinessException("ORDER_NOT_FOUND", "订单不存在");
        }
        if (!sellerId.equals(toLong(order.get("sellerId")))) {
            throw new BusinessException("ORDER_FORBIDDEN", "仅卖家可发货");
        }
        if (!"PAID".equalsIgnoreCase(safeText(order.get("status")))) {
            throw new BusinessException("ORDER_STATUS_INVALID", "当前订单不可发货");
        }
        if (orderMapper.shipOrder(orderId) == 0) {
            throw new BusinessException("ORDER_STATUS_INVALID", "订单发货失败，请刷新后重试");
        }
        appendSystemMessage(
                orderId,
                toLong(order.get("itemId")),
                sellerId,
                toLong(order.get("buyerId")),
                "TEXT",
                "卖家已发货，请留意收货并及时确认。"
        );
        return appendOrderDisplayFields(orderMapper.findOrderBase(orderId));
    }

    @Transactional
    public Map<String, Object> completeOrder(Long orderId) {
        Long buyerId = ensureActiveUserId();
        Map<String, Object> order = orderMapper.findOrderBase(orderId);
        if (order == null) {
            throw new BusinessException("ORDER_NOT_FOUND", "订单不存在");
        }
        if (!buyerId.equals(toLong(order.get("buyerId")))) {
            throw new BusinessException("ORDER_FORBIDDEN", "仅买家可确认收货");
        }
        if (!"SHIPPED".equalsIgnoreCase(safeText(order.get("status")))) {
            throw new BusinessException("ORDER_STATUS_INVALID", "当前订单不可确认收货");
        }
        if (orderMapper.completeOrder(orderId) == 0) {
            throw new BusinessException("ORDER_STATUS_INVALID", "确认收货失败，请刷新后重试");
        }
        appendSystemMessage(
                orderId,
                toLong(order.get("itemId")),
                buyerId,
                toLong(order.get("sellerId")),
                "TEXT",
                "买家已确认收货，本次交易已完成。"
        );
        return appendOrderDisplayFields(orderMapper.findOrderBase(orderId));
    }

    @Transactional
    public Map<String, Object> cancelOrder(Long orderId, String cancelReason) {
        Long buyerId = ensureActiveUserId();
        expireTimedOutOrders();
        String normalizedCancelReason = normalizeRequiredField(cancelReason, "ORDER_CANCEL_REASON_REQUIRED", "请填写取消原因");
        Map<String, Object> order = orderMapper.findOrderBase(orderId);
        if (order == null) {
            throw new BusinessException("ORDER_NOT_FOUND", "订单不存在");
        }
        if (!buyerId.equals(toLong(order.get("buyerId")))) {
            throw new BusinessException("ORDER_FORBIDDEN", "仅买家可取消订单");
        }
        if (!"PENDING_PAYMENT".equalsIgnoreCase(safeText(order.get("status")))) {
            throw new BusinessException("ORDER_STATUS_INVALID", "仅待支付订单可取消");
        }
        if (orderMapper.cancelOrder(orderId, normalizedCancelReason) == 0) {
            throw new BusinessException("ORDER_STATUS_INVALID", "订单取消失败，请刷新后重试");
        }
        itemMapper.restoreStockForOrder(toLong(order.get("itemId")));
        appendSystemMessage(
                orderId,
                toLong(order.get("itemId")),
                buyerId,
                toLong(order.get("sellerId")),
                "TEXT",
                "买家已取消订单，原因：" + normalizedCancelReason
        );
        return appendOrderDisplayFields(orderMapper.findOrderBase(orderId));
    }

    public List<Map<String, Object>> getReviews() {
        Long userId = ensureActiveUserId();
        if (toBoolean(userMapper.isAdminUser(userId))) {
            return orderMapper.findReviews();
        }
        return orderMapper.findReviewsByUser(userId);
    }

    @Transactional
    public Map<String, Object> createReview(Long orderId, int score, String content) {
        Long currentUserId = ensureActiveUserId();
        Map<String, Object> order = orderMapper.findOrderBase(orderId);
        if (order == null) {
            throw new BusinessException("ORDER_NOT_FOUND", "订单不存在");
        }
        Long buyerId = toLong(order.get("buyerId"));
        Long sellerId = toLong(order.get("sellerId"));
        if (!currentUserId.equals(buyerId) && !currentUserId.equals(sellerId)) {
            throw new BusinessException("ORDER_FORBIDDEN", "仅订单相关方可提交评价");
        }
        if (!"COMPLETED".equalsIgnoreCase(safeText(order.get("status")))) {
            throw new BusinessException("ORDER_STATUS_INVALID", "仅已完成订单可评价");
        }
        if (orderMapper.countReviewsByOrderAndUser(orderId, currentUserId) > 0) {
            throw new BusinessException("REVIEW_ALREADY_EXISTS", "该订单已提交过评价");
        }
        Map<String, Object> review = new LinkedHashMap<>();
        review.put("orderId", orderId);
        review.put("fromUser", currentUserId);
        review.put("toUser", currentUserId.equals(buyerId) ? sellerId : buyerId);
        review.put("score", score);
        review.put("content", content);
        orderMapper.insertReview(review);
        review.put("createdAt", OffsetDateTime.now().toString());
        appendSystemMessage(
                orderId,
                toLong(order.get("itemId")),
                currentUserId,
                currentUserId.equals(buyerId) ? sellerId : buyerId,
                "REVIEW",
                String.format("%s 评价了商品「%s」：%s 星，%s",
                        currentUserId.equals(buyerId) ? "买家" : "卖家",
                        safeText(order.get("itemTitle")),
                        score,
                        content)
        );
        return review;
    }

    public Map<String, Object> getDashboard() {
        ensureAdmin();
        return Map.of(
                "userCount", userMapper.countUsers(),
                "itemCount", itemMapper.countItems(),
                "orderCount", orderMapper.countOrders(),
                "pendingAuditCount", itemMapper.countPendingAudit(),
                "reportCount", reportMapper.countReports(),
                "pendingDisputeCount", orderMapper.countPendingDisputes()
        );
    }

    public Map<String, Object> checkNicknameAvailability(String nickname) {
        String normalizedNickname = normalizeNickname(nickname);
        return Map.of(
                "nickname", normalizedNickname,
                "available", userMapper.countByNickname(normalizedNickname) == 0
        );
    }

    @Transactional
    public Map<String, Object> registerWebUser(String nickname, String password) {
        String normalizedNickname = normalizeNickname(nickname);
        validatePassword(password, false);
        if (userMapper.countByNickname(normalizedNickname) > 0) {
            throw new BusinessException("NICKNAME_EXISTS", "该昵称已被占用，请更换后重试");
        }
        Map<String, Object> user = new LinkedHashMap<>();
        user.put("openid", "web_" + UUID.randomUUID().toString().replace("-", ""));
        user.put("nickname", normalizedNickname);
        userMapper.insertUser(user);
        String salt = PasswordCodec.generateSalt();
        userMapper.upsertUserPassword(
                toLong(user.get("id")),
                salt,
                PasswordCodec.encode(password.trim(), salt)
        );
        Map<String, Object> createdUser = userMapper.findById(toLong(user.get("id")));
        return buildLoginResult(createdUser, false);
    }

    public Map<String, Object> loginWithPassword(String account, String password, String loginType) {
        String normalizedAccount = safeText(account).trim();
        String normalizedPassword = safeText(password).trim();
        if (normalizedAccount.isBlank() || normalizedPassword.isBlank()) {
            throw new BusinessException("LOGIN_INVALID", "请输入账号和密码");
        }
        String normalizedLoginType = normalizeLoginType(loginType, normalizedAccount);
        Map<String, Object> user = resolvePasswordLoginUser(normalizedAccount, normalizedLoginType);
        if (user == null) {
            throw new BusinessException("LOGIN_FAILED", "账号或密码错误");
        }
        if ("ADMIN".equals(normalizedLoginType) && !toBoolean(user.get("isAdmin"))) {
            throw new BusinessException("LOGIN_FAILED", "管理员账号或密码错误");
        }
        String accountStatus = String.valueOf(user.get("status"));
        if ("DELETED".equalsIgnoreCase(accountStatus)) {
            throw new BusinessException("ACCOUNT_DELETED", "账号已被删除，无法登录");
        }
        if ("BANNED".equalsIgnoreCase(accountStatus)) {
            throw new BusinessException("ACCOUNT_BANNED", "账号已被封禁，无法登录");
        }
        String salt = safeText(user.get("salt"));
        String passwordHash = safeText(user.get("passwordHash"));
        if (salt.isBlank() || passwordHash.isBlank() || !PasswordCodec.matches(normalizedPassword, salt, passwordHash)) {
            throw new BusinessException("LOGIN_FAILED", "账号或密码错误");
        }
        return buildLoginResult(user, toBoolean(user.get("isAdmin")));
    }

    @Transactional
    public Map<String, Object> auditItem(Long itemId, String result, String reason, String userAction) {
        ensureAdmin();
        String normalizedResult = safeText(result).trim().toUpperCase(Locale.ROOT);
        if (!List.of("APPROVED", "REJECTED").contains(normalizedResult)) {
            throw new BusinessException("AUDIT_RESULT_INVALID", "审核结果仅支持 APPROVED 或 REJECTED");
        }
        String normalizedReason = safeText(reason).trim();
        String normalizedUserAction = safeText(userAction).trim().toUpperCase(Locale.ROOT);
        if (normalizedUserAction.isBlank()) {
            normalizedUserAction = "NONE";
        }
        if (!List.of("NONE", "WARNED", "BANNED").contains(normalizedUserAction)) {
            throw new BusinessException("AUDIT_ACTION_INVALID", "用户处理仅支持 NONE、WARNED、BANNED");
        }
        Map<String, Object> item = itemMapper.findItemBase(itemId);
        if (item == null) {
            throw new BusinessException("ITEM_NOT_FOUND", "商品不存在或无法审核");
        }
        int updated = itemMapper.updateAudit(itemId, normalizedResult, normalizedReason);
        if (updated == 0) {
            throw new BusinessException("ITEM_NOT_FOUND", "商品不存在或无法审核");
        }
        String handleResult = normalizedReason.isBlank()
                ? ("APPROVED".equals(normalizedResult) ? "举报审核通过，商品恢复上架" : "举报审核未通过，商品已打回")
                : normalizedReason;
        reportMapper.updateReportsByTarget("ITEM", itemId, normalizedResult, handleResult);
        Long sellerId = toLong(item.get("sellerId"));
        if (!"NONE".equals(normalizedUserAction) && !toBoolean(userMapper.isAdminUser(sellerId))) {
            userMapper.updateUserStatus(sellerId, normalizedUserAction);
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("itemId", itemId);
        data.put("result", normalizedResult);
        data.put("reason", normalizedReason);
        data.put("userAction", normalizedUserAction);
        data.put("handledAt", OffsetDateTime.now().toString());
        return data;
    }

    public Map<String, Object> getUserProfile() {
        Long userId = ensureActiveUserId();
        Map<String, Object> profile = userMapper.getProfile(userId);
        if (profile == null) {
            throw new BusinessException("PROFILE_NOT_FOUND", "用户信息不存在");
        }
        Object studentNo = profile.get("studentNo");
        if (studentNo instanceof String studentNoValue && !studentNoValue.isBlank()) {
            profile.put("studentNo", mask(studentNoValue));
        }
        return profile;
    }

    @Transactional
    public Map<String, Object> updateNickname(String nickname) {
        Long userId = ensureActiveUserId();
        String normalizedNickname = normalizeNickname(nickname);
        if (userMapper.countByNicknameExcludeUser(normalizedNickname, userId) > 0) {
            throw new BusinessException("NICKNAME_EXISTS", "该昵称已被占用，请更换后重试");
        }
        int updated = userMapper.updateNickname(userId, normalizedNickname);
        if (updated == 0) {
            throw new BusinessException("USER_NOT_FOUND", "用户不存在");
        }
        Map<String, Object> user = userMapper.findById(userId);
        return buildLoginResult(user, toBoolean(userMapper.isAdminUser(userId)));
    }

    @Transactional
    public Map<String, Object> updatePassword(String oldPassword, String newPassword) {
        Long userId = ensureActiveUserId();
        validatePassword(newPassword, true);
        Map<String, Object> passwordAuth = userMapper.getPasswordAuth(userId);
        String storedSalt = passwordAuth == null ? "" : safeText(passwordAuth.get("salt"));
        String storedPasswordHash = passwordAuth == null ? "" : safeText(passwordAuth.get("passwordHash"));
        if (storedSalt.isBlank() || storedPasswordHash.isBlank()
                || !PasswordCodec.matches(safeText(oldPassword).trim(), storedSalt, storedPasswordHash)) {
            throw new BusinessException("PASSWORD_INVALID", "原密码校验失败");
        }
        String normalizedNewPassword = newPassword.trim();
        if (PasswordCodec.matches(normalizedNewPassword, storedSalt, storedPasswordHash)) {
            throw new BusinessException("PASSWORD_UNCHANGED", "新密码不能与原密码相同");
        }
        String newSalt = PasswordCodec.generateSalt();
        userMapper.upsertUserPassword(userId, newSalt, PasswordCodec.encode(normalizedNewPassword, newSalt));
        return createResult("userId", userId, "PASSWORD_UPDATED");
    }

    @Transactional
    public Map<String, Object> updateAvatar(String avatarUrl) {
        Long userId = ensureActiveUserId();
        String normalizedAvatarUrl = safeText(avatarUrl).trim();
        if (normalizedAvatarUrl.isBlank()) {
            throw new BusinessException("AVATAR_EMPTY", "头像地址不能为空");
        }
        int updated = userMapper.updateAvatarUrl(userId, normalizedAvatarUrl);
        if (updated == 0) {
            throw new BusinessException("USER_NOT_FOUND", "用户不存在");
        }
        return getUserProfile();
    }

    public List<Map<String, Object>> getFavorites() {
        return favoriteMapper.findFavorites(ensureActiveUserId());
    }

    @Transactional
    public Map<String, Object> addFavorite(Long itemId) {
        favoriteMapper.insertFavorite(ensureActiveUserId(), itemId);
        return createResult("itemId", itemId, "FAVORITED");
    }

    @Transactional
    public Map<String, Object> removeFavorite(Long itemId) {
        favoriteMapper.deleteFavorite(ensureActiveUserId(), itemId);
        return createResult("itemId", itemId, "REMOVED");
    }

    public List<Map<String, Object>> getBrowseHistory() {
        return historyMapper.findBrowseHistory(ensureActiveUserId());
    }

    public List<Map<String, Object>> getReports() {
        ensureAdmin();
        return reportMapper.findReports();
    }

    @Transactional
    public Map<String, Object> createReport(String targetType, Long targetId, String reason) {
        Long reporterId = ensureActiveUserId();
        String normalizedTargetType = safeText(targetType).trim().toUpperCase(Locale.ROOT);
        String normalizedReason = safeText(reason).trim();
        if (!"ITEM".equals(normalizedTargetType)) {
            throw new BusinessException("REPORT_TARGET_INVALID", "当前仅支持举报商品");
        }
        if (normalizedReason.isBlank()) {
            throw new BusinessException("REPORT_REASON_REQUIRED", "请填写举报原因");
        }
        Map<String, Object> item = itemMapper.findItemBase(targetId);
        if (item == null) {
            throw new BusinessException("ITEM_NOT_FOUND", "商品不存在");
        }
        if (reporterId.equals(toLong(item.get("sellerId")))) {
            throw new BusinessException("REPORT_SELF_FORBIDDEN", "不能举报自己发布的商品");
        }
        if (!"ON_SALE".equalsIgnoreCase(safeText(item.get("status")))) {
            throw new BusinessException("REPORT_ITEM_INVALID", "当前商品无法举报");
        }
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("reporterId", reporterId);
        report.put("targetType", normalizedTargetType);
        report.put("targetId", targetId);
        report.put("reason", normalizedReason);
        reportMapper.insertReport(report);
        if (itemMapper.hideItemForReport(targetId, normalizedReason) == 0) {
            throw new BusinessException("REPORT_ITEM_INVALID", "当前商品无法举报");
        }
        report.put("status", "PENDING");
        report.put("createdAt", OffsetDateTime.now().toString());
        return report;
    }

    public List<Map<String, Object>> getWantedPosts() {
        return wantedMapper.findWantedPosts();
    }

    public List<Map<String, Object>> getMessages(Long orderId, Long itemId) {
        Long userId = ensureActiveUserId();
        return messageMapper.findVisibleMessages(userId, orderId, itemId);
    }

    @Transactional
    public Map<String, Object> sendMessage(Long orderId, Long itemId, Long toUserId, String content) {
        Long currentUserId = ensureActiveUserId();
        String normalizedContent = content == null ? "" : content.trim();
        if (normalizedContent.isBlank()) {
            throw new BusinessException("MESSAGE_EMPTY", "消息内容不能为空");
        }

        Long normalizedOrderId = orderId;
        Long normalizedItemId = itemId;
        Long actualToUserId = toUserId;
        String itemTitle = "";

        if (normalizedOrderId != null) {
            Map<String, Object> order = orderMapper.findOrderBase(normalizedOrderId);
            if (order == null) {
                throw new BusinessException("ORDER_NOT_FOUND", "订单不存在");
            }
            Long buyerId = toLong(order.get("buyerId"));
            Long sellerId = toLong(order.get("sellerId"));
            if (!currentUserId.equals(buyerId) && !currentUserId.equals(sellerId)) {
                throw new BusinessException("ORDER_FORBIDDEN", "仅订单相关方可发送消息");
            }
            normalizedItemId = toLong(order.get("itemId"));
            actualToUserId = currentUserId.equals(buyerId) ? sellerId : buyerId;
            Map<String, Object> item = itemMapper.findItemBase(normalizedItemId);
            itemTitle = item == null ? "" : safeText(item.get("title"));
        } else if (normalizedItemId != null) {
            Map<String, Object> item = itemMapper.findItemBase(normalizedItemId);
            if (item == null) {
                throw new BusinessException("ITEM_NOT_FOUND", "商品不存在");
            }
            Long sellerId = toLong(item.get("sellerId"));
            if (currentUserId.equals(sellerId)) {
                if (actualToUserId == null || currentUserId.equals(actualToUserId)) {
                    throw new BusinessException("CHAT_TARGET_REQUIRED", "请先选择聊天对象");
                }
            } else {
                actualToUserId = sellerId;
            }
            itemTitle = safeText(item.get("title"));
        } else {
            throw new BusinessException("MESSAGE_SCOPE_REQUIRED", "请选择聊天对象");
        }

        if (actualToUserId == null) {
            throw new BusinessException("CHAT_TARGET_REQUIRED", "请先选择聊天对象");
        }
        if (currentUserId.equals(actualToUserId)) {
            throw new BusinessException("CHAT_SELF_FORBIDDEN", "不能和自己聊天");
        }

        Long conversationId = ensureConversationId(currentUserId, actualToUserId);
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("conversationId", conversationId);
        message.put("fromUser", currentUserId);
        message.put("toUser", actualToUserId);
        message.put("orderId", normalizedOrderId);
        message.put("itemId", normalizedItemId);
        message.put("messageType", "text");
        message.put("content", normalizedContent);
        messageMapper.insertMessage(message);
        messageMapper.touchConversation(conversationId);
        message.put("createdAt", OffsetDateTime.now().toString());
        message.put("itemTitle", itemTitle);
        message.put("messageType", "TEXT");
        return message;
    }

    @Transactional
    public Map<String, Object> hideMessageThread(Long orderId, Long itemId) {
        Long userId = ensureActiveUserId();
        Long normalizedOrderId = orderId == null ? 0L : orderId;
        Long normalizedItemId = itemId == null ? 0L : itemId;
        if (normalizedOrderId == 0L && normalizedItemId == 0L) {
            throw new BusinessException("MESSAGE_SCOPE_REQUIRED", "请选择要删除的聊天记录");
        }
        if (normalizedOrderId != 0L) {
            Map<String, Object> order = orderMapper.findOrderBase(normalizedOrderId);
            if (order == null) {
                throw new BusinessException("ORDER_NOT_FOUND", "订单不存在");
            }
            Long buyerId = toLong(order.get("buyerId"));
            Long sellerId = toLong(order.get("sellerId"));
            if (!userId.equals(buyerId) && !userId.equals(sellerId)) {
                throw new BusinessException("ORDER_FORBIDDEN", "仅订单相关方可删除聊天记录");
            }
            normalizedItemId = toLong(order.get("itemId"));
        }
        messageMapper.upsertHiddenThread(userId, normalizedOrderId, normalizedItemId);
        return createResult("orderId", normalizedOrderId == 0L ? null : normalizedOrderId, "HIDDEN");
    }

    @Transactional
    public Map<String, Object> createWanted(String title, BigDecimal budget, String description) {
        Long userId = ensureActiveUserId();
        ensureCampusVerified(userId);
        Map<String, Object> wanted = new LinkedHashMap<>();
        wanted.put("userId", userId);
        wanted.put("title", title);
        wanted.put("budget", budget);
        wanted.put("description", description);
        wantedMapper.insertWantedPost(wanted);
        wanted.put("status", "OPEN");
        wanted.put("createdAt", OffsetDateTime.now().toString());
        return wanted;
    }

    public List<Map<String, Object>> getMyItems() {
        return itemMapper.findMyItems(ensureActiveUserId());
    }

    @Transactional
    public Map<String, Object> updateMyItemStatus(Long itemId, String status) {
        Long sellerId = ensureActiveUserId();
        Map<String, Object> item = itemMapper.findMyItemBase(itemId, sellerId);
        if (item == null) {
            throw new BusinessException("ITEM_UPDATE_FORBIDDEN", "只能操作自己发布的商品");
        }
        if ("ON_SALE".equalsIgnoreCase(status)) {
            Integer stock = toInteger(item.get("stock"));
            if (stock == null || stock < 1) {
                throw new BusinessException("ITEM_STOCK_EMPTY", "库存为 0 的商品不能重新上架");
            }
            String currentStatus = safeText(item.get("status"));
            if ("PENDING_REVIEW".equalsIgnoreCase(currentStatus) || "REJECTED".equalsIgnoreCase(currentStatus)) {
                throw new BusinessException("ITEM_AUDIT_REQUIRED", "被举报或打回的商品需等待管理员审核处理");
            }
        }
        int updated = itemMapper.updateMyItemStatus(itemId, sellerId, status);
        if (updated == 0) {
            throw new BusinessException("ITEM_UPDATE_FORBIDDEN", "只能操作自己发布的商品");
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("itemId", itemId);
        data.put("status", status);
        data.put("updatedAt", OffsetDateTime.now().toString());
        return data;
    }

    @Transactional
    public Map<String, Object> deleteMyItem(Long itemId) {
        int updated = itemMapper.deleteMyItem(itemId, ensureActiveUserId());
        if (updated == 0) {
            throw new BusinessException("ITEM_DELETE_FORBIDDEN", "只能删除自己发布的商品");
        }
        return createResult("itemId", itemId, "DELETED");
    }

    @Scheduled(fixedDelay = 60000, initialDelay = 60000)
    @Transactional
    public void expirePendingPaymentOrdersOnSchedule() {
        expireTimedOutOrders();
    }

    public List<Map<String, Object>> getAdminUsers() {
        ensureAdmin();
        return userMapper.findAdminUsers();
    }

    @Transactional
    public Map<String, Object> updateAdminUserStatus(Long userId, String status) {
        ensureAdmin();
        String normalizedStatus = safeText(status).trim().toUpperCase(Locale.ROOT);
        if (!List.of("ACTIVE", "WARNED", "BANNED").contains(normalizedStatus)) {
            throw new BusinessException("USER_STATUS_INVALID", "用户状态仅支持 ACTIVE、WARNED、BANNED");
        }
        if (toBoolean(userMapper.isAdminUser(userId))) {
            throw new BusinessException("ADMIN_STATUS_PROTECTED", "不能修改管理员账号状态");
        }
        int updated = userMapper.updateUserStatus(userId, normalizedStatus);
        if (updated == 0) {
            throw new BusinessException("USER_NOT_FOUND", "用户不存在");
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("userId", userId);
        data.put("status", normalizedStatus);
        data.put("handledAt", OffsetDateTime.now().toString());
        return data;
    }

    @Transactional
    public Map<String, Object> deleteAdminUser(Long userId) {
        Long adminId = ensureActiveUserId();
        ensureAdmin();
        if (adminId.equals(userId)) {
            throw new BusinessException("ADMIN_SELF_DELETE_FORBIDDEN", "不能删除当前登录管理员");
        }
        if (toBoolean(userMapper.isAdminUser(userId))) {
            throw new BusinessException("ADMIN_STATUS_PROTECTED", "不能删除管理员账号");
        }
        int updated = userMapper.markUserDeleted(userId, "已删除用户#" + userId);
        if (updated == 0) {
            throw new BusinessException("USER_NOT_FOUND", "用户不存在");
        }
        itemMapper.deleteItemsBySeller(userId);
        return createResult("userId", userId, "DELETED");
    }

    public List<Map<String, Object>> getDisputes() {
        Long userId = ensureActiveUserId();
        if (toBoolean(userMapper.isAdminUser(userId))) {
            return orderMapper.findDisputes();
        }
        return orderMapper.findDisputesByUser(userId);
    }

    @Transactional
    public Map<String, Object> createDispute(Long orderId, String reason) {
        Long currentUserId = ensureActiveUserId();
        String normalizedReason = normalizeRequiredField(reason, "DISPUTE_REASON_REQUIRED", "请填写纠纷原因");
        Map<String, Object> order = orderMapper.findOrderBase(orderId);
        if (order == null) {
            throw new BusinessException("ORDER_NOT_FOUND", "订单不存在");
        }
        Long buyerId = toLong(order.get("buyerId"));
        Long sellerId = toLong(order.get("sellerId"));
        if (!currentUserId.equals(buyerId) && !currentUserId.equals(sellerId)) {
            throw new BusinessException("ORDER_FORBIDDEN", "仅订单相关方可发起纠纷");
        }
        String orderStatus = safeText(order.get("status")).trim().toUpperCase(Locale.ROOT);
        if (!List.of("PAID", "SHIPPED", "COMPLETED").contains(orderStatus)) {
            throw new BusinessException("ORDER_STATUS_INVALID", "仅已支付、已发货或已完成订单可发起纠纷");
        }
        if (orderMapper.countOpenDisputesByOrder(orderId) > 0) {
            throw new BusinessException("DISPUTE_ALREADY_EXISTS", "该订单已有处理中纠纷");
        }
        Map<String, Object> dispute = new LinkedHashMap<>();
        dispute.put("orderId", orderId);
        dispute.put("complainantId", currentUserId);
        dispute.put("reason", normalizedReason);
        dispute.put("orderStatusSnapshot", orderStatus);
        orderMapper.insertDispute(dispute);
        orderMapper.updateOrderStatus(orderId, "DISPUTING");
        dispute.put("status", "PENDING");
        dispute.put("createdAt", OffsetDateTime.now().toString());
        appendSystemMessage(
                orderId,
                toLong(order.get("itemId")),
                currentUserId,
                currentUserId.equals(buyerId) ? sellerId : buyerId,
                "DISPUTE",
                String.format("%s 发起了纠纷：%s",
                        currentUserId.equals(buyerId) ? "买家" : "卖家",
                        normalizedReason)
        );
        return dispute;
    }

    @Transactional
    public Map<String, Object> hideOrderRecord(Long orderId) {
        Long userId = ensureActiveUserId();
        Map<String, Object> order = orderMapper.findOrderBase(orderId);
        if (order == null) {
            throw new BusinessException("ORDER_NOT_FOUND", "订单不存在");
        }
        Long buyerId = toLong(order.get("buyerId"));
        Long sellerId = toLong(order.get("sellerId"));
        if (!userId.equals(buyerId) && !userId.equals(sellerId)) {
            throw new BusinessException("ORDER_FORBIDDEN", "仅订单相关方可删除订单记录");
        }
        orderMapper.hideOrderForUser(userId, orderId);
        return createResult("orderId", orderId, "HIDDEN");
    }

    @Transactional
    public Map<String, Object> handleDispute(Long disputeId, String status, String handleResult) {
        Long adminId = ensureActiveUserId();
        ensureAdmin();
        String normalizedStatus = safeText(status).trim().toUpperCase(Locale.ROOT);
        String normalizedHandleResult = normalizeRequiredField(handleResult, "DISPUTE_HANDLE_RESULT_REQUIRED", "请填写处理结果");
        if (!List.of("PROCESSING", "RESOLVED_REFUND", "RESOLVED_COMPLETE", "REJECTED").contains(normalizedStatus)) {
            throw new BusinessException("DISPUTE_STATUS_INVALID", "纠纷状态仅支持 PROCESSING、RESOLVED_REFUND、RESOLVED_COMPLETE、REJECTED");
        }
        Map<String, Object> dispute = orderMapper.findDisputeById(disputeId);
        if (dispute == null) {
            throw new BusinessException("DISPUTE_NOT_FOUND", "纠纷不存在");
        }
        Map<String, Object> order = orderMapper.findOrderBase(toLong(dispute.get("orderId")));
        if (order == null) {
            throw new BusinessException("ORDER_NOT_FOUND", "订单不存在");
        }
        orderMapper.handleDispute(disputeId, normalizedStatus, normalizedHandleResult, adminId);
        if ("PROCESSING".equals(normalizedStatus)) {
            orderMapper.updateOrderStatus(toLong(dispute.get("orderId")), "DISPUTING");
        } else if ("RESOLVED_REFUND".equals(normalizedStatus)) {
            orderMapper.updateOrderStatus(toLong(dispute.get("orderId")), "REFUNDED");
            itemMapper.restoreStockForOrder(toLong(order.get("itemId")));
        } else if ("RESOLVED_COMPLETE".equals(normalizedStatus)) {
            orderMapper.completeOrder(toLong(dispute.get("orderId")));
        } else {
            orderMapper.updateOrderStatus(
                    toLong(dispute.get("orderId")),
                    safeText(dispute.get("orderStatusSnapshot")).trim().toUpperCase(Locale.ROOT)
            );
        }
        Long buyerId = toLong(order.get("buyerId"));
        Long sellerId = toLong(order.get("sellerId"));
        Long complainantId = toLong(dispute.get("complainantId"));
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("disputeId", disputeId);
        data.put("status", normalizedStatus);
        data.put("handleResult", normalizedHandleResult);
        data.put("handledAt", OffsetDateTime.now().toString());
        appendSystemMessage(
                toLong(dispute.get("orderId")),
                toLong(order.get("itemId")),
                complainantId,
                complainantId.equals(buyerId) ? sellerId : buyerId,
                "DISPUTE",
                "管理员已处理纠纷：" + normalizedHandleResult
        );
        return data;
    }

    @Transactional
    public Map<String, Object> wechatLogin(String code) {
        String openid = "wx_" + code;
        Map<String, Object> user = userMapper.findByOpenid(openid);
        if (user == null) {
            Map<String, Object> userData = new LinkedHashMap<>();
            userData.put("openid", openid);
            userData.put("nickname", "校园用户");
            userMapper.insertUser(userData);
            user = userData;
        }
        String status = String.valueOf(user.get("status"));
        if ("BANNED".equalsIgnoreCase(status)) {
            throw new BusinessException("ACCOUNT_BANNED", "账号已被封禁，无法登录");
        }
        LoginUser loginUser = new LoginUser(
                toLong(user.get("id")),
                String.valueOf(user.get("openid")),
                String.valueOf(user.get("nickname"))
        );
        return Map.of(
                "token", jwtTokenProvider.generateToken(loginUser),
                "userId", loginUser.userId(),
                "nickname", loginUser.nickname(),
                "isAdmin", toBoolean(userMapper.isAdminUser(loginUser.userId())),
                "loginTime", OffsetDateTime.now().toString()
        );
    }

    @Transactional
    public Map<String, Object> studentVerify(String studentNo, String campus) {
        Long userId = ensureActiveUserId();
        String normalizedStudentNo = studentNo.trim();
        String normalizedCampus = normalizeCampus(campus, true);
        if (!normalizedStudentNo.matches("\\d{8,12}")) {
            throw new BusinessException("INVALID_STUDENT_NO", "学号需为 8 到 12 位数字");
        }
        if (userMapper.countByStudentNoExcludeUser(normalizedStudentNo, userId) > 0) {
            throw new BusinessException("STUDENT_NO_EXISTS", "该学号已被其他账号绑定");
        }
        userMapper.upsertUserAuth(userId, normalizedStudentNo, normalizedCampus);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("verified", true);
        data.put("verifyStatus", "VERIFIED");
        data.put("studentNo", mask(normalizedStudentNo));
        data.put("campus", normalizedCampus);
        data.put("verifiedAt", OffsetDateTime.now().toString());
        return data;
    }

    public Map<String, Object> createResult(String key, Object value, String status) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put(key, value);
        result.put("status", status);
        result.put("time", OffsetDateTime.now().toString());
        return result;
    }

    private int expireTimedOutOrders() {
        int expiredCount = 0;
        for (Map<String, Object> order : orderMapper.findExpiredPendingPaymentOrders()) {
            Long orderId = toLong(order.get("id"));
            if (orderMapper.failExpiredOrder(orderId, ORDER_TIMEOUT_REASON) == 0) {
                continue;
            }
            expiredCount++;
            itemMapper.restoreStockForOrder(toLong(order.get("itemId")));
            appendSystemMessage(
                    orderId,
                    toLong(order.get("itemId")),
                    toLong(order.get("buyerId")),
                    toLong(order.get("sellerId")),
                    "TEXT",
                    String.format("订单超时未支付，系统已自动关闭，商品已重新上架。支付时限为 %d 分钟。", ORDER_PAYMENT_TIMEOUT_MINUTES)
            );
        }
        return expiredCount;
    }

    private Map<String, Object> buildLoginResult(Map<String, Object> user, boolean isAdmin) {
        Long userId = toLong(user.get("id"));
        String openid = safeText(user.get("openid"));
        String nickname = safeText(user.get("nickname"));
        LoginUser loginUser = new LoginUser(
                userId,
                openid.isBlank() ? "web_" + userId : openid,
                nickname
        );
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("token", jwtTokenProvider.generateToken(loginUser));
        result.put("userId", userId);
        result.put("nickname", nickname);
        result.put("avatarUrl", safeText(user.get("avatarUrl")));
        result.put("isAdmin", isAdmin);
        result.put("loginTime", OffsetDateTime.now().toString());
        return result;
    }

    private Map<String, Object> resolvePasswordLoginUser(String account, String loginType) {
        if ("STUDENT_NO".equals(loginType)) {
            return userMapper.findPasswordLoginUserByStudentNo(account);
        }
        return userMapper.findPasswordLoginUserByNickname(account);
    }

    private List<String> saveItemImages(Long itemId, List<String> imageUrls) {
        List<String> savedImageUrls = new ArrayList<>();
        if (imageUrls == null) {
            return savedImageUrls;
        }
        for (int i = 0; i < imageUrls.size(); i++) {
            String imageUrl = imageUrls.get(i);
            if (imageUrl == null || imageUrl.isBlank()) {
                continue;
            }
            itemMapper.insertItemImage(itemId, imageUrl.trim(), i);
            savedImageUrls.add(imageUrl.trim());
        }
        return savedImageUrls;
    }

    private Long ensureCurrentUserId() {
        LoginUser loginUser = UserContext.get();
        if (loginUser == null) {
            throw new BusinessException("UNAUTHORIZED", "未登录或登录已过期");
        }
        return loginUser.userId();
    }

    private String normalizeCampus(String campus, boolean required) {
        String normalizedCampus = campus == null ? "" : campus.trim();
        if (normalizedCampus.isBlank()) {
            if (required) {
                throw new BusinessException("INVALID_CAMPUS", "请选择校区");
            }
            return "";
        }
        if (!SUPPORTED_CAMPUSES.contains(normalizedCampus)) {
            throw new BusinessException("INVALID_CAMPUS", "校区仅支持望江校区或江安校区");
        }
        return normalizedCampus;
    }

    private String normalizeRequiredField(String value, String code, String message) {
        String normalizedValue = safeText(value).trim().replaceAll("\\s+", " ");
        if (normalizedValue.isBlank()) {
            throw new BusinessException(code, message);
        }
        return normalizedValue;
    }

    private String normalizePhone(String phone) {
        String normalizedPhone = normalizeRequiredField(phone, "ORDER_PHONE_REQUIRED", "请填写联系电话");
        if (!normalizedPhone.matches("\\d{6,20}")) {
            throw new BusinessException("ORDER_PHONE_INVALID", "联系电话需为 6 到 20 位数字");
        }
        return normalizedPhone;
    }

    private String normalizePaymentMethod(String paymentMethod) {
        String normalizedPaymentMethod = safeText(paymentMethod).trim().toUpperCase(Locale.ROOT);
        if (!SUPPORTED_PAYMENT_METHODS.contains(normalizedPaymentMethod)) {
            throw new BusinessException("PAYMENT_METHOD_INVALID", "支付方式仅支持 WECHAT、ALIPAY、CAMPUS_CARD");
        }
        return normalizedPaymentMethod;
    }

    private String buildPaymentNo(Long orderId) {
        String date = OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return "PAY" + date + String.format("%06d", orderId);
    }

    private String normalizeNickname(String nickname) {
        String normalizedNickname = safeText(nickname).trim().replaceAll("\\s+", " ");
        if (normalizedNickname.length() < 2 || normalizedNickname.length() > 20) {
            throw new BusinessException("INVALID_NICKNAME", "昵称长度需为 2 到 20 个字符");
        }
        if ("校园用户".equals(normalizedNickname)) {
            throw new BusinessException("INVALID_NICKNAME", "该昵称为系统保留昵称，请更换后重试");
        }
        return normalizedNickname;
    }

    private void validatePassword(String password, boolean allowSameAsOldValidation) {
        String normalizedPassword = safeText(password).trim();
        if (normalizedPassword.length() < 6 || normalizedPassword.length() > 20) {
            throw new BusinessException("INVALID_PASSWORD", "密码长度需为 6 到 20 位");
        }
        if (normalizedPassword.contains(" ")) {
            throw new BusinessException("INVALID_PASSWORD", "密码不能包含空格");
        }
        if (!allowSameAsOldValidation && normalizedPassword.equalsIgnoreCase("123456")) {
            return;
        }
    }

    private String normalizeLoginType(String loginType, String account) {
        String normalizedLoginType = safeText(loginType).trim().toUpperCase(Locale.ROOT);
        if (normalizedLoginType.isBlank()) {
            return account.matches("\\d{8,12}") ? "STUDENT_NO" : "NICKNAME";
        }
        if (!SUPPORTED_LOGIN_TYPES.contains(normalizedLoginType)) {
            throw new BusinessException("LOGIN_TYPE_INVALID", "登录方式仅支持昵称、学号或管理员");
        }
        return normalizedLoginType;
    }

    private Map<String, Object> appendOrderDisplayFields(Map<String, Object> order) {
        Map<String, Object> enriched = new LinkedHashMap<>(order);
        Long orderId = toLong(order.get("id"));
        String status = safeText(order.get("status")).trim().toUpperCase(Locale.ROOT);
        OffsetDateTime createdTime = parseDateTime(order.get("createdAt"));
        if (createdTime == null) {
            createdTime = OffsetDateTime.now();
        }
        String date = createdTime.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        enriched.put("orderNo", "CT" + date + String.format("%06d", orderId));
        OffsetDateTime expireAt = createdTime.plusMinutes(ORDER_PAYMENT_TIMEOUT_MINUTES);
        long remainingSeconds = "PENDING_PAYMENT".equals(status)
                ? Math.max(0, Duration.between(OffsetDateTime.now(), expireAt).getSeconds())
                : 0L;
        enriched.put("paymentExpireAt", expireAt.toString());
        enriched.put("paymentTimeoutMinutes", ORDER_PAYMENT_TIMEOUT_MINUTES);
        enriched.put("paymentRemainingSeconds", remainingSeconds);
        return enriched;
    }

    private OffsetDateTime parseDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof OffsetDateTime offsetDateTime) {
            return offsetDateTime;
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime.atZone(ZoneId.systemDefault()).toOffsetDateTime();
        }
        if (value instanceof java.util.Date date) {
            return OffsetDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
        }
        String text = safeText(value).trim();
        if (text.isBlank() || "null".equalsIgnoreCase(text)) {
            return null;
        }
        try {
            return OffsetDateTime.parse(text);
        } catch (Exception ignored) {
            // Fallback for database datetime values like "2026-06-01 12:34:56".
        }
        try {
            return LocalDateTime.parse(text.replace(' ', 'T'))
                    .atZone(ZoneId.systemDefault())
                    .toOffsetDateTime();
        } catch (Exception ignored) {
            return null;
        }
    }

    private void appendSystemMessage(Long orderId, Long itemId, Long fromUser, Long toUser, String messageType, String content) {
        Long conversationId = ensureConversationId(fromUser, toUser);
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("conversationId", conversationId);
        message.put("fromUser", fromUser);
        message.put("toUser", toUser);
        message.put("orderId", orderId);
        message.put("itemId", itemId);
        message.put("messageType", "text");
        message.put("content", buildCompatMessageContent(messageType, content));
        messageMapper.insertMessage(message);
        messageMapper.touchConversation(conversationId);
    }

    private Long ensureConversationId(Long userA, Long userB) {
        Long conversationId = messageMapper.findConversationId(userA, userB);
        if (conversationId != null) {
            return conversationId;
        }
        Map<String, Object> conversation = new LinkedHashMap<>();
        conversation.put("userA", userA);
        conversation.put("userB", userB);
        messageMapper.insertConversation(conversation);
        return toLong(conversation.get("id"));
    }

    private String buildCompatMessageContent(String messageType, String content) {
        String normalizedType = safeText(messageType).trim().toUpperCase();
        if ("REVIEW".equals(normalizedType)) {
            return "REVIEW::" + content;
        }
        if ("DISPUTE".equals(normalizedType)) {
            return "DISPUTE::" + content;
        }
        return content;
    }

    private Long ensureActiveUserId() {
        Long userId = ensureCurrentUserId();
        Map<String, Object> user = userMapper.findById(userId);
        if (user == null) {
            throw new BusinessException("USER_NOT_FOUND", "用户不存在");
        }
        if ("DELETED".equalsIgnoreCase(String.valueOf(user.get("status")))) {
            throw new BusinessException("ACCOUNT_DELETED", "账号已被删除");
        }
        if ("BANNED".equalsIgnoreCase(String.valueOf(user.get("status")))) {
            throw new BusinessException("ACCOUNT_BANNED", "账号已被封禁");
        }
        return userId;
    }

    private void ensureCampusVerified(Long userId) {
        if (!toBoolean(userMapper.isCampusVerified(userId))) {
            throw new BusinessException("CAMPUS_VERIFY_REQUIRED", "请先完成校园认证后再使用该功能");
        }
    }

    private void ensureAdmin() {
        Long userId = ensureActiveUserId();
        if (!toBoolean(userMapper.isAdminUser(userId))) {
            throw new BusinessException("ADMIN_REQUIRED", "仅管理员可执行该操作");
        }
    }

    private Long toLong(Object value) {
        return ((Number) value).longValue();
    }

    private Integer toInteger(Object value) {
        return value == null ? null : ((Number) value).intValue();
    }

    private boolean toBoolean(Object value) {
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        if (value instanceof Number numberValue) {
            return numberValue.intValue() != 0;
        }
        String text = safeText(value).trim();
        return "true".equalsIgnoreCase(text) || "1".equals(text) || "yes".equalsIgnoreCase(text);
    }

    private String mask(String studentNo) {
        if (studentNo.length() <= 4) {
            return "****";
        }
        return studentNo.substring(0, 2) + "****" + studentNo.substring(studentNo.length() - 2);
    }

    private String safeText(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}

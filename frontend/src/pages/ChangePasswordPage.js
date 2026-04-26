import React, { useCallback, useEffect, useState } from "react";
import { Button, Card, Form, Input, Space, Typography, message } from "antd";
import { ArrowLeftOutlined, LockOutlined } from "@ant-design/icons";
import { useNavigate } from "react-router-dom";
import { changePassword, getCurrentUser } from "../api/auth";

const { Paragraph, Title } = Typography;

export default function ChangePasswordPage({ forced = false }) {
  const [loading, setLoading] = useState(false);
  const [forcePasswordChange, setForcePasswordChange] = useState(forced);
  const navigate = useNavigate();

  useEffect(() => {
    async function loadCurrentUser() {
      const user = await getCurrentUser().catch(() => null);
      setForcePasswordChange(forced || Boolean(user?.forcePasswordChange));
    }

    loadCurrentUser();
  }, [forced]);

  const handleSubmit = useCallback(async (values) => {
    setLoading(true);
    try {
      await changePassword(values.oldPassword, values.newPassword);
      message.success("密码修改成功，请重新登录。");
      navigate("/login", { replace: true });
      window.location.reload();
    } catch (error) {
      message.error(error?.response?.data || error.message || "密码修改失败");
    } finally {
      setLoading(false);
    }
  }, [navigate]);

  const card = (
    <Card bordered={false} style={{ width: "100%", maxWidth: 560, margin: "0 auto" }}>
      <Space direction="vertical" size={20} style={{ width: "100%" }}>
        <div>
          {!forcePasswordChange ? (
            <Button type="link" icon={<ArrowLeftOutlined />} onClick={() => navigate(-1)} style={{ paddingLeft: 0 }}>
              返回上一页
            </Button>
          ) : null}
          <Title level={3} style={{ marginBottom: 8 }}>
            {forcePasswordChange ? "首次登录修改密码" : "修改密码"}
          </Title>
          <Paragraph type="secondary" style={{ marginBottom: 0 }}>
            {forcePasswordChange
              ? "管理员已重置你的密码，请先设置新密码后再进入系统。"
              : "修改成功后，当前登录状态会失效，需要使用新密码重新登录。"}
          </Paragraph>
        </div>

        <Form layout="vertical" onFinish={handleSubmit}>
          <Form.Item
            name="oldPassword"
            label="当前密码"
            rules={[{ required: true, message: "请输入当前密码" }]}
          >
            <Input.Password prefix={<LockOutlined />} placeholder="请输入当前密码" allowClear />
          </Form.Item>
          <Form.Item
            name="newPassword"
            label="新密码"
            rules={[
              { required: true, message: "请输入新密码" },
              { min: 6, message: "新密码至少 6 位" },
            ]}
          >
            <Input.Password prefix={<LockOutlined />} placeholder="请输入新密码" allowClear />
          </Form.Item>
          <Form.Item
            name="confirmPassword"
            label="确认新密码"
            dependencies={["newPassword"]}
            rules={[
              { required: true, message: "请再次输入新密码" },
              ({ getFieldValue }) => ({
                validator(_, value) {
                  if (!value || getFieldValue("newPassword") === value) {
                    return Promise.resolve();
                  }
                  return Promise.reject(new Error("两次输入的新密码不一致"));
                },
              }),
            ]}
          >
            <Input.Password prefix={<LockOutlined />} placeholder="请再次输入新密码" allowClear />
          </Form.Item>
          <Form.Item style={{ marginBottom: 0 }}>
            <Space>
              {!forcePasswordChange ? <Button onClick={() => navigate(-1)}>取消</Button> : null}
              <Button type="primary" htmlType="submit" loading={loading}>
                确认修改
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Space>
    </Card>
  );

  if (forcePasswordChange) {
    return (
      <div
        style={{
          minHeight: "100vh",
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          padding: 24,
          background: "#f5f7fa",
        }}
      >
        {card}
      </div>
    );
  }

  return card;
}

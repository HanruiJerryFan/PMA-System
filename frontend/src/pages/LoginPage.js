import React, { useCallback, useEffect, useState } from "react";
import { Button, Card, Col, Form, Input, Row, Typography, message } from "antd";
import { LockOutlined, LoginOutlined, UserAddOutlined, UserOutlined } from "@ant-design/icons";
import { useLocation, useNavigate } from "react-router-dom";
import { getCurrentUser, login } from "../api/auth";

const { Title, Text } = Typography;

export default function LoginPage({ setLoggedIn }) {
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();

  useEffect(() => {
    async function checkAuth() {
      const user = await getCurrentUser();
      if (user) {
        setLoggedIn(true, user);
        if (user.forcePasswordChange) {
          navigate("/change-password", { replace: true });
        } else {
          const from = location.state?.from?.pathname || "/home";
          navigate(from, { replace: true });
        }
      }
    }

    checkAuth();
  }, [location.state, navigate, setLoggedIn]);

  const handleSubmit = useCallback(async (values) => {
    setLoading(true);
    try {
      await login(values.username, values.password);
      const currentUser = await getCurrentUser();
      message.success("登录成功");
      setLoggedIn(true, currentUser);
      if (currentUser?.forcePasswordChange) {
        navigate("/change-password", { replace: true });
      } else {
        navigate("/home", { replace: true });
      }
    } catch (error) {
      if (error.response?.status === 401) {
        message.error("用户名或密码错误");
      } else if (error.response?.status === 403) {
        message.error(error.response?.data || "当前账号不可登录");
      } else {
        message.error(error.response?.data || error.message || "登录失败");
      }
    } finally {
      setLoading(false);
    }
  }, [navigate, setLoggedIn]);

  return (
    <div
      style={{
        minHeight: "100vh",
        background: "linear-gradient(135deg, #14532d 0%, #1d4ed8 100%)",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        padding: 20,
      }}
    >
      <Row justify="center" style={{ width: "100%" }}>
        <Col xs={22} sm={16} md={12} lg={8} xl={6}>
          <Card
            bordered={false}
            style={{ borderRadius: 16, boxShadow: "0 16px 48px rgba(15, 23, 42, 0.2)" }}
            styles={{ body: { padding: 36 } }}
          >
            <div style={{ textAlign: "center", marginBottom: 32 }}>
              <Title level={2} style={{ color: "#0f172a", marginBottom: 8 }}>
                销售管理系统
              </Title>
              <Text type="secondary">请输入账号密码进入系统</Text>
            </div>

            <Form name="login" onFinish={handleSubmit} layout="vertical" size="large">
              <Form.Item name="username" rules={[{ required: true, message: "请输入用户名" }]}>
                <Input prefix={<UserOutlined style={{ color: "#94a3b8" }} />} placeholder="用户名" allowClear />
              </Form.Item>

              <Form.Item name="password" rules={[{ required: true, message: "请输入密码" }]}>
                <Input.Password prefix={<LockOutlined style={{ color: "#94a3b8" }} />} placeholder="密码" allowClear />
              </Form.Item>

              <Form.Item style={{ marginBottom: 12 }}>
                <Button
                  type="primary"
                  htmlType="submit"
                  loading={loading}
                  icon={<LoginOutlined />}
                  block
                  size="large"
                  style={{ height: 46, borderRadius: 10 }}
                >
                  登录
                </Button>
              </Form.Item>

              <div style={{ display: "flex", justifyContent: "flex-end", marginBottom: 20 }}>
                <Button
                  type="link"
                  icon={<UserAddOutlined />}
                  onClick={() => navigate("/register")}
                  style={{ paddingInline: 0 }}
                >
                  注册账号
                </Button>
              </div>
            </Form>
          </Card>
        </Col>
      </Row>
    </div>
  );
}

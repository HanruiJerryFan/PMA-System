import React, { useState } from "react";
import { Button, Card, Col, Form, Input, Row, Space, Typography, message } from "antd";
import { LockOutlined, LoginOutlined, SafetyOutlined, UserAddOutlined, UserOutlined } from "@ant-design/icons";
import { useNavigate } from "react-router-dom";
import { register } from "../api/auth";

const { Title, Text } = Typography;

export default function RegisterPage() {
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const validatePassword = (password) =>
    /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,20}$/.test(password);

  const handleSubmit = async (values) => {
    setLoading(true);
    try {
      await register(values.username, values.password, values.realName);
      message.success("注册成功，2 秒后跳转到登录页");
      setTimeout(() => navigate("/login"), 2000);
    } catch (error) {
      message.error(`注册失败：${error.message}`);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div
      style={{
        minHeight: "100vh",
        background: "linear-gradient(135deg, #667eea 0%, #764ba2 100%)",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        padding: 20,
      }}
    >
      <Row justify="center" style={{ width: "100%" }}>
        <Col xs={22} sm={16} md={12} lg={8} xl={6}>
          <Card
            style={{ borderRadius: 12, boxShadow: "0 8px 32px rgba(0, 0, 0, 0.1)", border: "none" }}
            styles={{ body: { padding: 40 } }}
          >
            <div style={{ textAlign: "center", marginBottom: 32 }}>
              <Title level={2} style={{ color: "#001529", marginBottom: 8 }}>
                用户注册
              </Title>
              <Text type="secondary">创建一个新的系统账号</Text>
            </div>

            <Form name="register" onFinish={handleSubmit} layout="vertical" size="large">
              <Form.Item
                name="username"
                rules={[
                  { required: true, message: "请输入用户名" },
                  { min: 3, message: "用户名至少 3 个字符" },
                  { max: 20, message: "用户名不能超过 20 个字符" },
                ]}
              >
                <Input prefix={<UserOutlined style={{ color: "#999" }} />} placeholder="请输入用户名" allowClear />
              </Form.Item>

              <Form.Item
                name="realName"
                rules={[
                  { required: true, message: "请输入真实姓名" },
                  { min: 2, message: "真实姓名至少 2 个字符" },
                ]}
              >
                <Input prefix={<SafetyOutlined style={{ color: "#999" }} />} placeholder="请输入真实姓名" allowClear />
              </Form.Item>

              <Form.Item
                name="password"
                rules={[
                  { required: true, message: "请输入密码" },
                  {
                    validator: (_, value) =>
                      !value || validatePassword(value)
                        ? Promise.resolve()
                        : Promise.reject(new Error("密码需为 8-20 位，且同时包含大小写字母、数字和特殊字符")),
                  },
                ]}
                hasFeedback
              >
                <Input.Password prefix={<LockOutlined style={{ color: "#999" }} />} placeholder="请输入密码" allowClear />
              </Form.Item>

              <Form.Item
                name="confirmPassword"
                dependencies={["password"]}
                rules={[
                  { required: true, message: "请再次输入密码" },
                  ({ getFieldValue }) => ({
                    validator(_, value) {
                      if (!value || getFieldValue("password") === value) {
                        return Promise.resolve();
                      }
                      return Promise.reject(new Error("两次输入的密码不一致"));
                    },
                  }),
                ]}
                hasFeedback
              >
                <Input.Password prefix={<LockOutlined style={{ color: "#999" }} />} placeholder="请再次输入密码" allowClear />
              </Form.Item>

              <Form.Item style={{ marginBottom: 16 }}>
                <Button
                  type="primary"
                  htmlType="submit"
                  loading={loading}
                  icon={<UserAddOutlined />}
                  block
                  size="large"
                  style={{ height: 48, borderRadius: 8, fontSize: 16 }}
                >
                  注册
                </Button>
              </Form.Item>

              <div style={{ textAlign: "center" }}>
                <Space>
                  <Text type="secondary">已有账号？</Text>
                  <Button type="link" icon={<LoginOutlined />} onClick={() => navigate("/login")} style={{ padding: 0 }}>
                    去登录
                  </Button>
                </Space>
              </div>
            </Form>

            <div style={{ marginTop: 24, padding: 16, backgroundColor: "#f5f5f5", borderRadius: 8 }}>
              <Text type="secondary" style={{ fontSize: 12 }}>
                <strong>密码规则：</strong>
                <br />
                1. 长度 8-20 位
                <br />
                2. 至少包含一个大写字母
                <br />
                3. 至少包含一个小写字母
                <br />
                4. 至少包含一个数字
                <br />
                5. 至少包含一个特殊字符：`@$!%*?&`
              </Text>
            </div>
          </Card>
        </Col>
      </Row>
    </div>
  );
}

/*
 * Copyright 2002-2008 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.jms.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.channel.PollableChannel;
import org.springframework.integration.core.Message;
import org.springframework.integration.jms.JmsInboundGateway;
import org.springframework.jms.connection.JmsTransactionManager;
import org.springframework.jms.listener.AbstractMessageListenerContainer;

/**
 * @author Mark Fisher
 */
public class JmsInboundGatewayParserTests {

	@Test
	public void testGatewayWithConnectionFactoryAndDestination() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsGatewayWithConnectionFactoryAndDestination.xml", this.getClass());
		PollableChannel channel = (PollableChannel) context.getBean("requestChannel");
		JmsInboundGateway gateway = (JmsInboundGateway) context.getBean("jmsGateway");
		assertEquals(JmsInboundGateway.class, gateway.getClass());
		context.start();
		Message<?> message = channel.receive(3000);
		assertNotNull("message should not be null", message);
		assertEquals("message-driven-test", message.getPayload());
		context.stop();
	}

	@Test
	public void testGatewayWithConnectionFactoryAndDestinationName() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsGatewayWithConnectionFactoryAndDestinationName.xml", this.getClass());
		PollableChannel channel = (PollableChannel) context.getBean("requestChannel");
		JmsInboundGateway gateway = (JmsInboundGateway) context.getBean("jmsGateway");
		assertEquals(JmsInboundGateway.class, gateway.getClass());
		context.start();
		Message<?> message = channel.receive(3000);
		assertNotNull("message should not be null", message);
		assertEquals("message-driven-test", message.getPayload());
		context.stop();
	}

	@Test
	public void testGatewayWithMessageConverter() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsGatewayWithMessageConverter.xml", this.getClass());
		PollableChannel channel = (PollableChannel) context.getBean("requestChannel");
		JmsInboundGateway gateway = (JmsInboundGateway) context.getBean("jmsGateway");
		assertEquals(JmsInboundGateway.class, gateway.getClass());
		context.start();
		Message<?> message = channel.receive(3000);
		assertNotNull("message should not be null", message);
		assertEquals("converted-test-message", message.getPayload());
		context.stop();
	}

	@Test
	public void testGatewayWithDefaultExtractPayload() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsGatewaysWithExtractPayloadAttributes.xml", this.getClass());
		JmsInboundGateway gateway = (JmsInboundGateway) context.getBean("defaultGateway");
		DirectFieldAccessor accessor = new DirectFieldAccessor(gateway);
		assertEquals(Boolean.TRUE, accessor.getPropertyValue("extractReplyPayload"));
	}

	@Test
	public void testGatewayWithExtractReplyPayloadTrue() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsGatewaysWithExtractPayloadAttributes.xml", this.getClass());
		JmsInboundGateway gateway = (JmsInboundGateway) context.getBean("extractReplyPayloadTrue");
		DirectFieldAccessor accessor = new DirectFieldAccessor(gateway);
		assertEquals(Boolean.TRUE, accessor.getPropertyValue("extractReplyPayload"));
	}

	@Test
	public void testGatewayWithExtractReplyPayloadFalse() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsGatewaysWithExtractPayloadAttributes.xml", this.getClass());
		JmsInboundGateway gateway = (JmsInboundGateway) context.getBean("extractReplyPayloadFalse");
		DirectFieldAccessor accessor = new DirectFieldAccessor(gateway);
		assertEquals(Boolean.FALSE, accessor.getPropertyValue("extractReplyPayload"));
	}

	@Test
	public void testGatewayWithExtractRequestPayloadTrue() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsGatewaysWithExtractPayloadAttributes.xml", this.getClass());
		JmsInboundGateway gateway = (JmsInboundGateway) context.getBean("extractRequestPayloadTrue");
		DirectFieldAccessor accessor = new DirectFieldAccessor(gateway);
		assertEquals(Boolean.TRUE, accessor.getPropertyValue("extractRequestPayload"));
	}

	@Test
	public void testGatewayWithExtractRequestPayloadFalse() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsGatewaysWithExtractPayloadAttributes.xml", this.getClass());
		JmsInboundGateway gateway = (JmsInboundGateway) context.getBean("extractRequestPayloadFalse");
		DirectFieldAccessor accessor = new DirectFieldAccessor(gateway);
		assertEquals(Boolean.FALSE, accessor.getPropertyValue("extractRequestPayload"));
	}

	@Test(expected=BeanDefinitionStoreException.class)
	public void testGatewayWithConnectionFactoryOnly() {
		try {
			new ClassPathXmlApplicationContext("jmsGatewayWithConnectionFactoryOnly.xml", this.getClass());
		}
		catch (RuntimeException e) {
			assertEquals(BeanCreationException.class, e.getCause().getClass());
			throw e;
		}
	}

	@Test(expected=BeanDefinitionStoreException.class)
	public void testGatewayWithEmptyConnectionFactory() {
		try {
			new ClassPathXmlApplicationContext("jmsGatewayWithEmptyConnectionFactory.xml", this.getClass());
		}
		catch (RuntimeException e) {
			assertEquals(BeanCreationException.class, e.getCause().getClass());
			throw e;
		}
	}

	@Test
	public void testGatewayWithDefaultConnectionFactory() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsGatewayWithDefaultConnectionFactory.xml", this.getClass());
		PollableChannel channel = (PollableChannel) context.getBean("requestChannel");
		JmsInboundGateway gateway = (JmsInboundGateway) context.getBean("jmsGateway");
		assertEquals(JmsInboundGateway.class, gateway.getClass());
		context.start();
		Message<?> message = channel.receive(3000);
		assertNotNull("message should not be null", message);
		assertEquals("message-driven-test", message.getPayload());
		context.stop();
	}

	@Test
	public void testTransactionManagerIsNullByDefault() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsGatewayTransactionManagerTests.xml", this.getClass());
		JmsInboundGateway gateway = (JmsInboundGateway) context.getBean("gatewayWithoutTransactionManager");
		DirectFieldAccessor accessor = new DirectFieldAccessor(gateway);
		assertNull(accessor.getPropertyValue("transactionManager"));
	}

	@Test
	public void testGatewayWithTransactionManagerReference() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsGatewayTransactionManagerTests.xml", this.getClass());
		JmsInboundGateway gateway = (JmsInboundGateway) context.getBean("gatewayWithTransactionManager");
		DirectFieldAccessor accessor = new DirectFieldAccessor(gateway);
		Object txManager = accessor.getPropertyValue("transactionManager");
		assertEquals(JmsTransactionManager.class, txManager.getClass());
		assertEquals(context.getBean("txManager"), txManager);
		assertEquals(context.getBean("testConnectionFactory"), ((JmsTransactionManager) txManager).getConnectionFactory());
	}

	@Test
	public void testGatewayWithConcurrentConsumers() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsGatewayWithContainerSettings.xml", this.getClass());
		JmsInboundGateway gateway = (JmsInboundGateway) context.getBean("gatewayWithConcurrentConsumers");
		gateway.start();
		AbstractMessageListenerContainer container = (AbstractMessageListenerContainer)
				new DirectFieldAccessor(gateway).getPropertyValue("container");
		assertEquals(3, new DirectFieldAccessor(container).getPropertyValue("concurrentConsumers"));
		gateway.stop();
	}

	@Test
	public void testGatewayWithMaxConcurrentConsumers() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsGatewayWithContainerSettings.xml", this.getClass());
		JmsInboundGateway gateway = (JmsInboundGateway) context.getBean("gatewayWithMaxConcurrentConsumers");
		gateway.start();
		AbstractMessageListenerContainer container = (AbstractMessageListenerContainer)
				new DirectFieldAccessor(gateway).getPropertyValue("container");
		assertEquals(22, new DirectFieldAccessor(container).getPropertyValue("maxConcurrentConsumers"));
		gateway.stop();
	}

	@Test
	public void testGatewayWithMaxMessagesPerTask() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsGatewayWithContainerSettings.xml", this.getClass());
		JmsInboundGateway gateway = (JmsInboundGateway) context.getBean("gatewayWithMaxMessagesPerTask");
		gateway.start();
		AbstractMessageListenerContainer container = (AbstractMessageListenerContainer)
				new DirectFieldAccessor(gateway).getPropertyValue("container");
		assertEquals(99, new DirectFieldAccessor(container).getPropertyValue("maxMessagesPerTask"));
		gateway.stop();
	}

	@Test
	public void testGatewayWithIdleTaskExecutionLimit() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsGatewayWithContainerSettings.xml", this.getClass());
		JmsInboundGateway gateway = (JmsInboundGateway) context.getBean("gatewayWithIdleTaskExecutionLimit");
		gateway.start();
		AbstractMessageListenerContainer container = (AbstractMessageListenerContainer)
				new DirectFieldAccessor(gateway).getPropertyValue("container");
		assertEquals(7, new DirectFieldAccessor(container).getPropertyValue("idleTaskExecutionLimit"));
		gateway.stop();
	}

}

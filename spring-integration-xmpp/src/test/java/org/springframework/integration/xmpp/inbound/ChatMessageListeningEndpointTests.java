/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.integration.xmpp.inbound;

import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.StringReader;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smackx.gcm.packet.GcmPacketExtension;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.xmlpull.v1.XmlPullParser;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.xmpp.core.XmppContextUtils;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.ErrorMessage;

/**
 * @author Oleg Zhurakousky
 * @author Gunnar Hillert
 * @author Florian Schmaus
 * @author Artem Bilan
 */
public class ChatMessageListeningEndpointTests {


	@Test
	/*
	 * Should add/remove StanzaListener when endpoint started/stopped
	 */
	public void testLifecycle() {
		final Set<StanzaListener> packetListSet = new HashSet<StanzaListener>();
		XMPPConnection connection = mock(XMPPConnection.class);
		ChatMessageListeningEndpoint endpoint = new ChatMessageListeningEndpoint(connection);

		willAnswer(invocation -> {
			packetListSet.add(invocation.getArgumentAt(0, StanzaListener.class));
			return null;
		}).given(connection)
				.addAsyncStanzaListener(Mockito.any(StanzaListener.class), Mockito.any(StanzaFilter.class));

		willAnswer(invocation -> {
			packetListSet.remove(invocation.getArguments()[0]);
			return null;
		}).given(connection)
				.removeAsyncStanzaListener(Mockito.any(StanzaListener.class));

		assertEquals(0, packetListSet.size());
		endpoint.setOutputChannel(new QueueChannel());
		endpoint.setBeanFactory(mock(BeanFactory.class));
		endpoint.afterPropertiesSet();
		endpoint.start();
		assertEquals(1, packetListSet.size());
		endpoint.stop();
		assertEquals(0, packetListSet.size());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNonInitializationFailure() {
		ChatMessageListeningEndpoint endpoint = new ChatMessageListeningEndpoint(mock(XMPPConnection.class));
		endpoint.start();
	}

	@Test
	public void testWithImplicitXmppConnection() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.registerSingleton(XmppContextUtils.XMPP_CONNECTION_BEAN_NAME, mock(XMPPConnection.class));
		ChatMessageListeningEndpoint endpoint = new ChatMessageListeningEndpoint();
		endpoint.setBeanFactory(bf);
		endpoint.setOutputChannel(new QueueChannel());
		endpoint.afterPropertiesSet();
		assertNotNull(TestUtils.getPropertyValue(endpoint, "xmppConnection"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNoXmppConnection() {
		ChatMessageListeningEndpoint endpoint = new ChatMessageListeningEndpoint();
		endpoint.setBeanFactory(mock(BeanFactory.class));
		endpoint.afterPropertiesSet();
	}

	@Test
	public void testWithErrorChannel() throws NotConnectedException {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		XMPPConnection connection = mock(XMPPConnection.class);
		bf.registerSingleton(XmppContextUtils.XMPP_CONNECTION_BEAN_NAME, connection);

		ChatMessageListeningEndpoint endpoint = new ChatMessageListeningEndpoint();

		DirectChannel outChannel = new DirectChannel();
		outChannel.subscribe(message -> {
			throw new RuntimeException("ooops");
		});
		PollableChannel errorChannel = new QueueChannel();
		endpoint.setBeanFactory(bf);
		endpoint.setOutputChannel(outChannel);
		endpoint.setErrorChannel(errorChannel);
		endpoint.afterPropertiesSet();
		StanzaListener listener = (StanzaListener) TestUtils.getPropertyValue(endpoint, "stanzaListener");
		Message smackMessage = new Message("kermit@frog.com");
		smackMessage.setBody("hello");
		smackMessage.setThread("1234");
		listener.processPacket(smackMessage);

		ErrorMessage msg =
				(ErrorMessage) errorChannel.receive();
		assertEquals("hello", ((MessagingException) msg.getPayload()).getFailedMessage().getPayload());
	}

	@Test
	public void testExpression() throws Exception {
		TestXMPPConnection testXMPPConnection = new TestXMPPConnection();

		QueueChannel inputChannel = new QueueChannel();

		ChatMessageListeningEndpoint endpoint = new ChatMessageListeningEndpoint(testXMPPConnection);
		SpelExpressionParser parser = new SpelExpressionParser();
		endpoint.setPayloadExpression(parser.parseExpression("#root"));
		endpoint.setOutputChannel(inputChannel);
		endpoint.setBeanFactory(mock(BeanFactory.class));
		endpoint.afterPropertiesSet();
		endpoint.start();

		Message smackMessage = new Message();
		smackMessage.setBody("foo");

		XmlPullParser xmlPullParser = PacketParserUtils.newXmppParser(new StringReader(smackMessage.toString()));
		xmlPullParser.next();
		testXMPPConnection.parseAndProcessStanza(xmlPullParser);

		org.springframework.messaging.Message<?> receive = inputChannel.receive(10000);
		assertNotNull(receive);

		Object payload = receive.getPayload();
		assertThat(payload, instanceOf(Message.class));
		assertEquals(smackMessage.getStanzaId(), ((Message) payload).getStanzaId());
		assertEquals(smackMessage.getBody(), ((Message) payload).getBody());

		Log logger = Mockito.spy(TestUtils.getPropertyValue(endpoint, "logger", Log.class));
		given(logger.isInfoEnabled()).willReturn(true);
		final CountDownLatch logLatch = new CountDownLatch(1);
		willAnswer(invocation -> {
			Object result = invocation.callRealMethod();
			logLatch.countDown();
			return result;
		}).given(logger).info(anyString());

		new DirectFieldAccessor(endpoint).setPropertyValue("logger", logger);

		endpoint.setPayloadExpression(null);

		smackMessage = new Message();
		xmlPullParser = PacketParserUtils.newXmppParser(new StringReader(smackMessage.toString()));
		xmlPullParser.next();
		testXMPPConnection.parseAndProcessStanza(xmlPullParser);

		ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);

		assertTrue(logLatch.await(10, TimeUnit.SECONDS));

		verify(logger).info(argumentCaptor.capture());

		assertEquals("The XMPP Message [" + smackMessage + "] with empty body is ignored.",
				argumentCaptor.getValue());

		endpoint.stop();
	}

	@Test
	public void testGcmExtension() throws Exception {
		String data = "{\n" +
				"      \"to\":\"me\",\n" +
				"     \"notification\": {\n" +
				"        \"title\": \"Something interesting\",\n" +
				"        \"text\": \"Here we go\"\n" +
				"           },\n" +
				"      \"time_to_live\":\"600\"\n" +
				"      }\n" +
				"}";
		GcmPacketExtension packetExtension = new GcmPacketExtension(data);
		Message smackMessage = new Message();
		smackMessage.addExtension(packetExtension);

		TestXMPPConnection testXMPPConnection = new TestXMPPConnection();

		QueueChannel inputChannel = new QueueChannel();

		ChatMessageListeningEndpoint endpoint = new ChatMessageListeningEndpoint(testXMPPConnection);
		Expression payloadExpression = new SpelExpressionParser().parseExpression("#extension.json");
		endpoint.setPayloadExpression(payloadExpression);
		endpoint.setOutputChannel(inputChannel);
		endpoint.setBeanFactory(mock(BeanFactory.class));
		endpoint.afterPropertiesSet();
		endpoint.start();

		XmlPullParser xmlPullParser = PacketParserUtils.newXmppParser(new StringReader(smackMessage.toString()));
		xmlPullParser.next();
		testXMPPConnection.parseAndProcessStanza(xmlPullParser);

		org.springframework.messaging.Message<?> receive = inputChannel.receive(10000);
		assertNotNull(receive);

		assertEquals(data, receive.getPayload());

		endpoint.stop();
	}

	private static class TestXMPPConnection extends XMPPTCPConnection {

		TestXMPPConnection() {
			super(null);
		}

		@Override
		protected void parseAndProcessStanza(XmlPullParser parser) throws Exception {
			super.parseAndProcessStanza(parser);
		}

	}



}

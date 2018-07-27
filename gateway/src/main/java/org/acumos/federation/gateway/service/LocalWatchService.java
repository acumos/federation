/*-
 * ===============LICENSE_START=======================================================
 * Acumos
 * ===================================================================================
 * Copyright (C) 2017 AT&T Intellectual Property & Tech Mahindra. All rights reserved.
 * ===================================================================================
 * This Acumos software file is distributed by AT&T and Tech Mahindra
 * under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * This file is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ===============LICENSE_END=========================================================
 */

package org.acumos.federation.gateway.service;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.acumos.federation.gateway.config.EELFLoggerDelegate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Local implementations get their info from local file(s), we help them here a
 * bit.
 */
@Service
public class LocalWatchService {

	private Map<URI, Consumer<URI>> sources = new HashMap<URI, Consumer<URI>>();
	private WatchService sourceWatcher = null;

	private static final EELFLoggerDelegate log = EELFLoggerDelegate.getLogger(MethodHandles.lookup().lookupClass());

	public void watchOn(URI theUri, Consumer<URI> theHandler) {
		this.sources.put(theUri, theHandler);
		setupSourceWatcher(theUri);
	}

	protected void setupSourceWatcher(URI theSource) {

		if (this.sourceWatcher == null)
			log.warn(EELFLoggerDelegate.errorLogger, "source watcher not available ");

		if ("file".equals(theSource.getScheme())) {
			// we can only watch directories ..
			Path sourcePath = Paths.get(theSource).getParent();
			try {
				sourcePath.register(this.sourceWatcher, StandardWatchEventKinds.ENTRY_MODIFY);
			} catch (IOException iox) {
				log.warn(EELFLoggerDelegate.errorLogger, "Failed to setup source watcher for " + theSource, iox);
			}
		}
	}

	@PostConstruct
	public void initLocalService() {
		log.debug(EELFLoggerDelegate.debugLogger, "init local service");

		try {
			this.sourceWatcher = FileSystems.getDefault().newWatchService();
		} catch (IOException iox) {
			log.warn(EELFLoggerDelegate.debugLogger, "Failed to setup source watcher: " + iox);
			this.sourceWatcher = null;
		}

		// Done
		log.debug(EELFLoggerDelegate.debugLogger, "local service available");
	}

	@PreDestroy
	public void cleanupLocalService() {
		if (this.sourceWatcher != null) {
			try {
				this.sourceWatcher.close();
			} catch (IOException iox) {
			}
		}
	}

	@Scheduled(fixedRateString = "${peer.local.interval:60}000")
	protected void updateInfo() {
		log.info(EELFLoggerDelegate.debugLogger, "ckecking for updates");
		if (this.sourceWatcher == null) {
			log.debug(EELFLoggerDelegate.debugLogger, "source watcher not in place");
			return;
		}

		// we are looking for modifications in the parent, now we have to match them
		// to the actual URIs

		WatchKey key = null;
		while ((key = this.sourceWatcher.poll()) != null) {

			// Path sourcePath = Paths.get(this.sourceUri).getFileName();
			for (WatchEvent<?> event : key.pollEvents()) {
				final Path changedPath = (Path) event.context();
				URI uri = changedPath.toUri();
				log.info(EELFLoggerDelegate.debugLogger, "Local update: " + uri);
				Consumer c = sources.get(uri);
				if (c != null)
					c.accept(uri);
			}

			key.reset();
		}
	}

}

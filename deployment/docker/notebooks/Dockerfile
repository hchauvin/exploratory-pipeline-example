FROM alpine@sha256:b276d875eeed9c7d3f1cfa7edb06b22ed22b14219a7d67c52c56612330348239 as reflow

RUN apk update && apk add wget && rm -rf /var/cache/apk/*

ENV REFLOW_VERSION=0.6.8

RUN wget https://github.com/grailbio/reflow/releases/download/reflow${REFLOW_VERSION}/reflow${REFLOW_VERSION}.linux.amd64 -q && \
    chmod +x reflow${REFLOW_VERSION}.linux.amd64 && \
    mv reflow${REFLOW_VERSION}.linux.amd64 /usr/local/bin/reflow && \
    reflow version

# ======================================================================================================================

FROM polynote/polynote:0.3.6-2.12@sha256:5c3f5aaf4851a548d72ecd5554c71d788973cf13f74decbb096288daebaceb1f

USER root

RUN wget http://apache.crihan.fr/dist/spark/spark-3.0.0-preview2/spark-3.0.0-preview2-bin-hadoop2.7.tgz -q && \
  tar zxpf spark-3.0.0-preview2-bin-hadoop2.7.tgz && mv spark-3.0.0-preview2-bin-hadoop2.7 /opt/spark && rm spark-3.0.0-preview2-bin-hadoop2.7.tgz

ENV SPARK_HOME=/opt/spark
ENV PATH=/opt/spark/bin:${PATH}

COPY --from=reflow /usr/local/bin/reflow /usr/local/bin/reflow

RUN apt-get update \
	&& apt-get install -y --no-install-recommends \
		ed \
		less \
		locales \
		vim-tiny \
		wget \
		ca-certificates \
		fonts-texgyre \
	&& rm -rf /var/lib/apt/lists/*
RUN echo "en_US.UTF-8 UTF-8" >> /etc/locale.gen \
	&& locale-gen en_US.utf8 \
	&& /usr/sbin/update-locale LANG=en_US.UTF-8
ENV LC_ALL en_US.UTF-8
ENV LANG en_US.UTF-8
RUN echo "deb http://http.debian.net/debian sid main" > /etc/apt/sources.list.d/debian-unstable.list \
        && echo 'APT::Default-Release "testing";' > /etc/apt/apt.conf.d/default
ENV R_BASE_VERSION 3.6.3
RUN apt-get update \
	&& apt-get install -t unstable -y --no-install-recommends \
		littler \
                r-cran-littler \
		r-base=${R_BASE_VERSION}-* \
		r-base-dev=${R_BASE_VERSION}-* \
		r-recommended=${R_BASE_VERSION}-* \
	&& ln -s /usr/lib/R/site-library/littler/examples/install.r /usr/local/bin/install.r \
	&& ln -s /usr/lib/R/site-library/littler/examples/install2.r /usr/local/bin/install2.r \
	&& ln -s /usr/lib/R/site-library/littler/examples/installGithub.r /usr/local/bin/installGithub.r \
	&& ln -s /usr/lib/R/site-library/littler/examples/testInstalled.r /usr/local/bin/testInstalled.r \
	&& install.r docopt \
	&& rm -rf /tmp/downloaded_packages/ /tmp/*.rds \
	&& rm -rf /var/lib/apt/lists/*

RUN pip3 install -r ./polynote/requirements.txt

RUN R CMD javareconf && Rscript -e 'install.packages(c("magrittr", "base64enc", "reshape2", "rJava", "BiocManager")); BiocManager::install("limma"); BiocManager::install("ComplexHeatmap")'
RUN Rscript -e 'install.packages(c("evaluate", "repr"))'

COPY . /opt/app

CMD [ "--config", "/opt/app/polynote.config.yml" ]
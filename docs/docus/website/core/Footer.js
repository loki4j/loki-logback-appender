/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

const React = require('react');

class Footer extends React.Component {
  docUrl(doc) {
    const baseUrl = this.props.config.baseUrl;
    const docsUrl = this.props.config.docsUrl;
    const docsPart = `${docsUrl ? `${docsUrl}/` : ''}`;
    return `${baseUrl}${docsPart}${doc}`;
  }

  render() {
    return (
      <footer className="nav-footer" id="footer">
        <section className="sitemap">
          <a href={this.props.config.baseUrl} className="nav-home">
            {this.props.config.footerIcon && (
              <img
                src={this.props.config.baseUrl + this.props.config.footerIcon}
                alt={this.props.config.title}
                width="66"
                height="58"
              />
            )}
          </a>
          <div>
            <h5>Docs</h5>
            {/*<a href={this.docUrl('quick-start')}>
              Getting Started
            </a>*/}
            <a href={this.docUrl(this.props.config.startDoc)}>
              Reference
            </a>
          </div>
          <div>
            <h5>Community</h5>
            <a
              className="github-button"
              href={this.props.config.repoUrl}
              data-icon="octicon-star"
              data-count-href="/loki4j/loki-logback-appender/stargazers"
              data-show-count="true"
              data-count-aria-label="# stargazers on GitHub"
              aria-label="Star this project on GitHub">
              GitHub
            </a>
            <a
              href="${this.props.config.linkedInProfile}"
              target="_blank"
              rel="noreferrer noopener">
              LinkedIn
            </a>
          </div>
          <div>
            <h5>More</h5>
            <a 
              href="https://docusaurus.io/">
              Powered by Docusaurus
            </a>
          </div>
        </section>
        <section className="copyright">{this.props.config.copyright}</section>
      </footer>
    );
  }
}

module.exports = Footer;

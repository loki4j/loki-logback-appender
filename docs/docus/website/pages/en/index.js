/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

const React = require('react');

const CompLibrary = require('../../core/CompLibrary.js');
const CodeTabsMarkdownBlock = require('../../core/CodeTabsMarkdownBlock.js');
const Doc = require('../../core/Doc.js');
const fs = require('fs');

const MarkdownBlock = CompLibrary.MarkdownBlock; /* Used to read markdown */
const Container = CompLibrary.Container;
const GridBlock = CompLibrary.GridBlock;

class HomeSplash extends React.Component {
  render() {
    const {siteConfig, language = ''} = this.props;
    const {baseUrl, docsUrl} = siteConfig;
    const docsPart = `${docsUrl ? `${docsUrl}/` : ''}`;
    const langPart = `${language ? `${language}/` : ''}`;
    const docUrl = (doc) => `${baseUrl}${docsPart}${langPart}${doc}`;

    const SplashContainer = (props) => (
      <div className="homeContainer">
        <div className="homeSplashFade">
          <div className="wrapper homeWrapper">{props.children}</div>
        </div>
      </div>
    );

    /*const Logo = (props) => (
      <div className="projectLogo">
        <img src={props.img_src} alt="Project Logo" />
      </div>
    );*/

    const ProjectTitle = (props) => (
      <h2 className="projectTitle">
        {props.title}
        <small>{props.tagline}</small>
      </h2>
    );

    const PromoSection = (props) => (
      <div className="section promoSection">
        <div className="promoRow">
          <div className="pluginRowBlock">{props.children}</div>
        </div>
      </div>
    );

    const Button = (props) => (
      <div className="pluginWrapper buttonWrapper">
        <a className="button" href={props.href} target={props.target}>
          {props.children}
        </a>
      </div>
    );

    return (
      <SplashContainer>
        {/*<Logo img_src={`${baseUrl}img/undraw_monitor.svg`} />*/}
        <div className="inner">
          <ProjectTitle tagline={siteConfig.tagline} title={siteConfig.title} />
          <PromoSection>
            <Button href="#quick-start">Quick start</Button>
            {/*<Button href="#features-start">Features</Button>*/}
            <Button href={docUrl(siteConfig.startDoc)}>Documentation</Button>
          </PromoSection>
        </div>
      </SplashContainer>
    );
  }
}

class Index extends React.Component {
  render() {
    const {config: siteConfig, language = ''} = this.props;
    const {baseUrl} = siteConfig;

    const Block = (props) => (
      <Container
        padding={['bottom', 'top']}
        id={props.id}
        background={props.background}>
        <GridBlock
          align="center"
          contents={props.children}
          layout={props.layout}
        />
      </Container>
    );

    const indexMd = fs
        .readFileSync('../docs/index.md', 'utf-8')
        .replace(/%version%/g, siteConfig.artifactVersion);
    const IndexContent = () => (
      <div
        className="productShowcaseSection paddingBottom"
        style={{textAlign: 'left'}}>
        <Doc
          metadata={{'custom_edit_url': null}}
          content={indexMd}
          config={this.props.config}
          source=''
          hideTitle={true}
          title='index'
          version=''
          language=''
        />
      </div>
    );

    const JsonProtoduf = () => (
      <Block id="features-start" background="dark">
        {[
          {
            content:
              'With Loki4j you can try out both JSON and Protobuf API for sending log records to Loki.',
            image: `${baseUrl}img/undraw_choice_9385.svg`,
            imageAlign: 'right',
            title: 'Support for both JSON and Protobuf formats',
          },
        ]}
      </Block>
    );

    const OptionalOrder = () => (
        <Block background="light">
          {[
            {
              content:
                'In order to prevent log records loss, Loki4j can sort log records by timestamp inside each batch, ' + 
                'so they will not be rejected by Loki with \'entry out of order\' error.',
              image: `${baseUrl}img/undraw_timeline.svg`,
              imageAlign: 'left',
              title: 'Optionally order log records before sending to Loki',
            },
          ]}
        </Block>
    );

    const LogbackPatterns = () => (
      <Block>
        {[
          {
            content:
              'Loki4j allows you to use all the power and flexibility of Logback patterns both for labels and messages.' +
              'Same patterns are used in Logback\'s standard ConsoleAppender or FileAppender, so you are probably familiar with the syntax.',
            image: `${baseUrl}img/undraw_code_typing_7jnv.svg`,
            imageAlign: 'right',
            title: 'Use Logback patterns for labels and messages formatting',
          },
        ]}
      </Block>
    );

    const NoJsonLib = () => (
        <Block background="dark">
          {[
            {
              content:
                'Instead of bundling with any JSON library (e.g. Jackson), Loki4j comes with a small part of JSON ' +
                'rendering functionality it needs embedded.',
              image: `${baseUrl}img/undraw_Throw_away_re_x60k.svg`,
              imageAlign: 'left',
              title: 'No JSON library bundled',
            },
          ]}
        </Block>
      );

    const ZeroDeps = () => (
      <Block background="light">
        {[
          {
            content:
              'Loki4j does not bring any new transitive dependencies to your project, assuming that you already use ' +
              'logback-classic for logging.',
            image: `${baseUrl}img/undraw_void.svg`,
            imageAlign: 'right',
            title: 'Zero-dependency',
          },
        ]}
      </Block>
    );

    const Features = () => (
      <Block layout="fourColumn">
        {[
          {
            content: 'This is the content of my feature',
            image: `${baseUrl}img/undraw_react.svg`,
            imageAlign: 'top',
            title: 'Feature One',
          },
          {
            content: 'The content of my second feature',
            image: `${baseUrl}img/undraw_operating_system.svg`,
            imageAlign: 'top',
            title: 'Feature Two',
          },
        ]}
      </Block>
    );

    const Showcase = () => {
      if ((siteConfig.users || []).length === 0) {
        return null;
      }

      const showcase = siteConfig.users
        .filter((user) => user.pinned)
        .map((user) => (
          <a href={user.infoLink} key={user.infoLink}>
            <img src={user.image} alt={user.caption} title={user.caption} />
          </a>
        ));

      const pageUrl = (page) =>
        baseUrl + (language ? `${language}/` : '') + page;

      return (
        <div className="productShowcaseSection paddingBottom">
          <h2>Who is Using This?</h2>
          <p>This project is used by all these people</p>
          <div className="logos">{showcase}</div>
          <div className="more-users">
            <a className="button" href={pageUrl('users.html')}>
              More {siteConfig.title} Users
            </a>
          </div>
        </div>
      );
    };

    return (
      <div>
        <HomeSplash siteConfig={siteConfig} language={language} />
        <div className="mainContainer">
          {/*<Features />*/}
          <IndexContent />
          {/*<JsonProtoduf />
          <OptionalOrder />
          <LogbackPatterns />
          <NoJsonLib />
          <ZeroDeps />
          <Showcase />*/}
        </div>
      </div>
    );
  }
}

module.exports = Index;

const React = require('react');
const fs = require('fs');

const CompLibrary = require('../../core/CompLibrary.js');
const Doc = require('../../core/Doc.js');

const Container = CompLibrary.Container;
const MarkdownBlock = CompLibrary.MarkdownBlock;

const readPart = (name) => fs.readFileSync(`parts/${name}.md`, 'utf-8');

function SupportUs(props) {
    const {config: siteConfig, language = ''} = props;

    const PromoSection = (props) => (
        <div className="section promoSection">
            <div className="promoRow">
                <div className="pluginRowBlock">{props.children}</div>
            </div>
        </div>
    );

    const Button = (props) => (
        <div className="pluginWrapper buttonWrapper">
            <a className="button buttonFilled" href={props.href} target={props.target}>
                {props.children}
            </a>
        </div>
    );

    return (
        <div className="docMainWrapper wrapper">
            <Container className="mainContainer documentContainer postContainer">
                <Doc
                    metadata={{ 'custom_edit_url': null }}
                    content={readPart('supportus-intro')}
                    config={siteConfig}
                    source=''
                    title='Support Loki4j'
                    version=''
                    language=''
                />

                <PromoSection>
                    <Button href="https://github.com/sponsors/nehaev">
                        Sponsor Loki4j project
                        <small>via GitHub Sponsors · @nehaev</small>
                    </Button>
                </PromoSection>

                <MarkdownBlock>{readPart('supportus-faq')}</MarkdownBlock>
            </Container>
        </div>
    );
}

module.exports = SupportUs;
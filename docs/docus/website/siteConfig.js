/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

// See https://docusaurus.io/docs/site-config for all the possible
// site configuration options.

// List of projects/orgs using your project for the users page.
const users = [
  {
    caption: 'User1',
    // You will need to prepend the image path with your baseUrl
    // if it is not '/', like: '/test-site/img/image.jpg'.
    image: '/img/undraw_open_source.svg',
    infoLink: 'https://www.facebook.com',
    pinned: true,
  },
];

const ghUrl = 'https://github.com/loki4j/loki-logback-appender';
const defaultDoc = 'configuration';

const siteConfig = {
  title: 'Loki4j Logback', // Title for your website.
  tagline: 'Pure Java Logback appender for Grafana Loki',
  url: 'https://loki4j.github.io', // Your website URL
  //baseUrl: '/',
  baseUrl: '/loki-logback-appender/', // Base URL for your project */

  // Used for publishing and more
  projectName: 'loki-logback-appender',
  organizationName: 'loki4j',
  // For top-level user or org sites, the organization is still the same.
  // e.g., for the https://JoelMarcey.github.io site, it would be set like...
  //   organizationName: 'JoelMarcey'

  // For no header links in the top nav bar -> headerLinks: [],
  headerLinks: [
    {doc: defaultDoc, label: 'Docs'},
    {href: `${ghUrl}/blob/main/CONTRIBUTING.md`, label: 'Contributing'},
    {page: 'help', label: 'Help'},
    {href: ghUrl, label: 'GitHub'},
  ],

  // If you have users set above, you add it here:
  users,

  /* path to images for header/footer */
  headerIcon: 'img/logo-gray.svg',
  footerIcon: 'img/logo.svg',
  favicon: 'img/logo-lite.svg',

  /* Colors for website */
  colors: {
    primaryColor: '#7e1324',
    secondaryColor: '#580d19',
  },

  /* Custom fonts for website */
  /*
  fonts: {
    myFont: [
      "Times New Roman",
      "Serif"
    ],
    myOtherFont: [
      "-apple-system",
      "system-ui"
    ]
  },
  */

  // This copyright info is used in /core/Footer.js and blog RSS/Atom feeds.
  copyright: `Copyright © 2020-${new Date().getFullYear()} Anton Nekhaev and Contributors`,

  highlight: {
    // Highlight.js theme to use for syntax highlighting in code blocks.
    theme: 'default',
  },

  // Add custom scripts here that would be placed in <script> tags.
  scripts: ['https://buttons.github.io/buttons.js'],

  // On page navigation for the current documentation page.
  onPageNav: 'separate',
  // No .html extensions for paths.
  cleanUrl: true,

  // Open Graph and Twitter card images.
  ogImage: 'img/undraw_online.svg',
  twitterImage: 'img/undraw_tweetstorm.svg',

  twitterUsername: 'arnehaev',
  gaTrackingId: 'G-0Z4FEXM003',
  gaGtag: true,

  // For sites with a sizable amount of content, set collapsible to true.
  // Expand/collapse the links and subcategories under categories.
  // docsSideNavCollapsible: true,

  // Show documentation's last contributor's name.
  // enableUpdateBy: true,

  // Show documentation's last update time.
  // enableUpdateTime: true,

  // You may provide arbitrary config keys to be used as needed by your
  // template. For example, if you need your repo's URL...
  repoUrl: ghUrl,
  startDoc: defaultDoc,

  artifactVersion: '1.6.0',
};

module.exports = siteConfig;

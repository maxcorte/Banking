import { useState } from 'react';

type Tab = 'mentions' | 'confidentialite';

/**
 * Pages légales affichées dans une fenêtre superposée (overlay), accessibles
 * depuis le pied de page — y compris avant connexion, comme l'exige le RGPD.
 *
 * ⚠️ Ce projet est une démonstration pédagogique : aucune donnée réelle, aucun
 * argent réel. Les textes ci-dessous le disent clairement et honnêtement.
 */
export function LegalPages({
  open,
  initialTab,
  onClose,
}: {
  open: boolean;
  initialTab: Tab;
  onClose: () => void;
}) {
  const [tab, setTab] = useState<Tab>(initialTab);

  if (!open) return null;

  return (
    <div className="legal-overlay" onClick={onClose}>
      <div
        className="legal-dialog"
        role="dialog"
        aria-modal="true"
        aria-label="Informations légales"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="legal-header">
          <div className="legal-tabs">
            <button
              type="button"
              className={tab === 'mentions' ? 'active' : ''}
              onClick={() => setTab('mentions')}
            >
              Mentions légales
            </button>
            <button
              type="button"
              className={tab === 'confidentialite' ? 'active' : ''}
              onClick={() => setTab('confidentialite')}
            >
              Confidentialité (RGPD)
            </button>
          </div>
          <button
            type="button"
            className="legal-close"
            aria-label="Fermer"
            onClick={onClose}
          >
            ✕
          </button>
        </div>

        <div className="legal-content">
          <p className="legal-banner">
            Projet étudiant à but pédagogique. Application de démonstration : les
            comptes, soldes et opérations sont fictifs. Aucun service bancaire
            réel n'est fourni, aucune somme d'argent réelle n'est en jeu.
          </p>

          {tab === 'mentions' ? <Mentions /> : <Confidentialite />}
        </div>
      </div>
    </div>
  );
}

function Mentions() {
  return (
    <section>
      <h2>Mentions légales</h2>

      <h3>Éditeur</h3>
      <p>
        Ce site est un projet personnel développé par Maxime Delcorte dans un
        cadre étudiant (master en informatique). Il n'a aucune vocation
        commerciale et ne constitue pas un établissement financier.
      </p>

      <h3>Nature du service</h3>
      <p>
        L'application simule le fonctionnement d'une banque (comptes, virements,
        historique) à des fins de démonstration technique. Elle n'est ni agréée
        ni supervisée par une autorité financière, et ne permet aucune opération
        réelle. Les fonds affichés sont fictifs.
      </p>

      <h3>Hébergement</h3>
      <p>
        Le site est hébergé sur un serveur privé virtuel (VPS) loué auprès de
        Hostinger, situé dans un centre de données de l'Union européenne. Le
        trafic est chiffré (HTTPS).
      </p>

      <h3>Contact</h3>
      <p>
        Pour toute question relative à ce projet, le contact se fait via le
        dépôt public du code source sur GitHub
        (github.com/maxcorte/Banking).
      </p>

      <h3>Propriété intellectuelle</h3>
      <p>
        Le code source est publié à titre pédagogique sur GitHub. Les
        bibliothèques tierces utilisées restent la propriété de leurs auteurs
        respectifs.
      </p>
    </section>
  );
}

function Confidentialite() {
  return (
    <section>
      <h2>Politique de confidentialité (RGPD)</h2>

      <p>
        Cette page décrit, en toute transparence, les données traitées par
        l'application de démonstration et la manière dont elles le sont.
      </p>

      <h3>Données collectées</h3>
      <ul>
        <li>
          Un <strong>nom d'utilisateur</strong>, choisi librement à
          l'inscription (il n'est pas nécessaire d'utiliser une vraie identité).
        </li>
        <li>
          Un <strong>mot de passe</strong>, stocké uniquement sous forme chiffrée
          (haché avec bcrypt) — jamais en clair.
        </li>
        <li>
          Les <strong>données fictives</strong> que tu crées dans l'app : comptes
          de démonstration, virements simulés, libellés d'opérations.
        </li>
      </ul>
      <p>
        Aucune donnée bancaire réelle, aucun numéro de carte, aucune information
        de paiement n'est demandée ni collectée.
      </p>

      <h3>Finalité</h3>
      <p>
        Ces données servent exclusivement à faire fonctionner la démonstration
        (t'authentifier et afficher tes comptes fictifs). Elles ne sont ni
        revendues, ni partagées, ni utilisées à des fins publicitaires.
      </p>

      <h3>Cookies</h3>
      <p>
        L'application utilise un unique cookie technique, strictement nécessaire
        à l'authentification (il maintient ta session connectée). Aucun cookie
        de suivi ni de mesure d'audience n'est déposé.
      </p>

      <h3>Conservation</h3>
      <p>
        Comme il s'agit d'un projet de démonstration, les données peuvent être
        réinitialisées à tout moment. Tu peux demander la suppression de ton
        compte de test à tout moment (voir ci-dessous).
      </p>

      <h3>Tes droits</h3>
      <p>
        Conformément au RGPD, tu disposes d'un droit d'accès, de rectification et
        de suppression de tes données. Pour l'exercer sur ce projet de
        démonstration, il suffit de supprimer ton compte de test ou d'en faire la
        demande via le dépôt GitHub du projet.
      </p>

      <h3>Sécurité</h3>
      <p>
        Les échanges sont chiffrés via HTTPS, les mots de passe sont hachés, et
        l'accès aux données de chaque utilisateur est cloisonné côté serveur.
      </p>
    </section>
  );
}

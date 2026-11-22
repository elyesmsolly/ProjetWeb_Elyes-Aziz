/** Contenu de la FAQ : chaque réponse décrit une règle réellement appliquée par le code. */
export const QUESTIONS = [
  {
    theme: 'Pour les clients',
    questions: [
      {
        q: 'Comment prendre un ticket ?',
        r: 'Connectez-vous, choisissez un établissement et une prestation, remplissez son formulaire, puis touchez « Prendre mon ticket ».',
      },
      {
        q: 'Comment l’attente est-elle estimée ?',
        r: 'On multiplie le nombre de personnes devant vous par la durée moyenne des derniers services, puis on divise par le nombre de postes ouverts. C’est une estimation, pas un rendez-vous.',
      },
      {
        q: 'Comment savoir que c’est mon tour ?',
        r: 'La page de votre ticket se met à jour en direct et affiche une alerte, avec un son et une vibration si votre appareil le permet. Gardez-la ouverte : Dourek n’envoie pas de SMS.',
      },
      {
        q: 'Puis-je laisser passer quelqu’un ?',
        r: 'Oui, tant que vous attendez. Vous reculez d’une place et gardez votre ticket, sauf si personne n’est derrière vous.',
      },
      {
        q: 'Comment annuler mon ticket ?',
        r: 'Sur la page du ticket, touchez « Annuler mon ticket », puis confirmez. Ce n’est plus possible une fois appelé.',
      },
      {
        q: 'Comment fonctionne la priorité ?',
        r: 'Elle est prévue pour l’âge, la grossesse et le handicap. Quand les deux files attendent, un client prioritaire passe après deux clients normaux au plus.',
      },
      {
        q: 'Que se passe-t-il si je suis absent ?',
        r: 'L’employé peut vous marquer absent. Le ticket est alors terminé et vous pouvez en reprendre un.',
      },
    ],
  },
  {
    theme: 'Pour les professionnels',
    questions: [
      {
        q: 'Comment créer ma file ?',
        r: 'Depuis « Espace pro », choisissez « Créer mon espace ». Cinq étapes suffisent : compte, établissement, services et postes, formulaire, confirmation.',
      },
      {
        q: 'Puis-je modifier le formulaire de mes clients ?',
        r: 'Oui. Chaque publication crée une version. Les clients déjà dans la file gardent celle qu’ils ont remplie.',
      },
      {
        q: 'Et les clients sans smartphone ?',
        r: 'L’employé les ajoute depuis son écran et leur donne leur numéro.',
      },
      {
        q: 'Que fait la mise en pause ?',
        r: 'Elle bloque les nouveaux tickets pris en ligne. Les clients déjà dans la file gardent leur place.',
      },
    ],
  },
];
